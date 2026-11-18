package cl.duoc.gestionoportunidades.repository

import cl.duoc.gestionoportunidades.data.local.OportunidadDao
import cl.duoc.gestionoportunidades.data.local.SeguimientoDao
import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.data.model.Seguimiento
import cl.duoc.gestionoportunidades.data.remote.OportunidadApiService
import cl.duoc.gestionoportunidades.data.remote.aDto
import cl.duoc.gestionoportunidades.data.remote.aOportunidad
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class OportunidadRepository(
    private val oportunidadDao: OportunidadDao,
    private val seguimientoDao: SeguimientoDao,
    private val api: OportunidadApiService
) {
    private val _oportunidades = MutableStateFlow<List<Oportunidad>>(emptyList())
    val oportunidades: StateFlow<List<Oportunidad>> = _oportunidades.asStateFlow()

    private val _sync = MutableStateFlow<EstadoSync>(EstadoSync.Inactivo)
    val sync: StateFlow<EstadoSync> = _sync.asStateFlow()

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val candado = Mutex()

    suspend fun refrescar() = withContext(Dispatchers.IO) {
        _oportunidades.value = oportunidadDao.listar()
    }

    suspend fun obtener(id: Long): Oportunidad? = withContext(Dispatchers.IO) { oportunidadDao.obtener(id) }

    suspend fun seguimientos(oportunidadId: Long): List<Seguimiento> =
        withContext(Dispatchers.IO) { seguimientoDao.listarPorOportunidad(oportunidadId) }

    suspend fun guardar(o: Oportunidad, responsable: String): Long = withContext(Dispatchers.IO) {
        val ahora = Fechas.marcaTiempo()
        val id = if (o.id == 0L) {
            val nuevoId = oportunidadDao.insertar(o.copy(actualizado = ahora, sincronizado = false))
            registrarAccion(nuevoId, "Investigación inicial", "Oportunidad registrada en la aplicación.", responsable, ahora)
            nuevoId
        } else {
            val anterior = oportunidadDao.obtener(o.id)
            oportunidadDao.actualizar(o.copy(actualizado = ahora, sincronizado = false, remoteId = anterior?.remoteId ?: o.remoteId))
            if (anterior != null && anterior.estado != o.estado) {
                registrarAccion(o.id, Catalogos.ACCION_CAMBIO_ESTADO, "${anterior.estado} → ${o.estado}", responsable, ahora)
            }
            if (anterior?.fotoRuta != null && anterior.fotoRuta != o.fotoRuta) File(anterior.fotoRuta).delete()
            o.id
        }
        _oportunidades.value = oportunidadDao.listar()
        sincronizarEnSegundoPlano()
        id
    }

    suspend fun cambiarEstado(o: Oportunidad, nuevoEstado: String, responsable: String) {
        if (o.estado == nuevoEstado) return
        guardar(o.copy(estado = nuevoEstado, propuestaEnviada = o.propuestaEnviada || nuevoEstado == "Propuesta enviada"), responsable)
    }

    suspend fun eliminar(o: Oportunidad) = withContext(Dispatchers.IO) {
        o.remoteId?.let { oportunidadDao.agregarEliminacionPendiente(it) }
        oportunidadDao.eliminar(o.id)
        o.fotoRuta?.let { File(it).delete() }
        _oportunidades.value = oportunidadDao.listar()
        sincronizarEnSegundoPlano()
    }

    suspend fun agregarSeguimiento(s: Seguimiento) = withContext(Dispatchers.IO) {
        val ahora = Fechas.marcaTiempo()
        seguimientoDao.insertar(s.copy(registradoEn = ahora))
        oportunidadDao.obtener(s.oportunidadId)?.let { o ->
            val estadoSugerido = if (o.estado in Catalogos.ESTADOS_SIN_CONTACTO && s.tipoAccion == "Primer contacto") {
                "Contacto realizado"
            } else o.estado
            if (estadoSugerido != o.estado) {
                seguimientoDao.insertar(
                    Seguimiento(
                        oportunidadId = o.id, tipoAccion = Catalogos.ACCION_CAMBIO_ESTADO,
                        fecha = Fechas.aIso(LocalDate.now()), resultado = "${o.estado} → $estadoSugerido",
                        proximaAccion = "", fechaProximo = null, responsable = s.responsable, registradoEn = ahora
                    )
                )
            }
            oportunidadDao.actualizar(
                o.copy(
                    estado = estadoSugerido,
                    propuestaEnviada = o.propuestaEnviada || s.tipoAccion == "Envío de propuesta",
                    proximoSeguimiento = s.fechaProximo,
                    actualizado = ahora,
                    sincronizado = false
                )
            )
        }
        _oportunidades.value = oportunidadDao.listar()
        sincronizarEnSegundoPlano()
    }

    fun sincronizarEnSegundoPlano() {
        alcance.launch { sincronizar() }
    }

    suspend fun sincronizar(): Boolean = withContext(Dispatchers.IO) {
        candado.withLock {
            _sync.value = EstadoSync.Sincronizando
            try {
                for (remoteId in oportunidadDao.listarEliminacionesPendientes()) {
                    val respuesta = api.eliminar(remoteId)
                    if (!respuesta.isSuccessful && respuesta.code() != 404) throw HttpException(respuesta)
                    oportunidadDao.quitarEliminacionPendiente(remoteId)
                }

                var rechazadas = 0
                for (o in oportunidadDao.listarPendientes()) {
                    if (!subir(o)) rechazadas++
                }

                val remotas = api.listar()
                val idsRemotos = remotas.mapNotNull { it.id }.toSet()
                val ahora = Fechas.marcaTiempo()
                for (dto in remotas) {
                    val remoteId = dto.id ?: continue
                    val local = oportunidadDao.obtenerPorRemoteId(remoteId)
                    when {
                        local == null -> {
                            val nuevoId = oportunidadDao.insertar(dto.aOportunidad(actualizado = ahora))
                            registrarAccion(nuevoId, "Investigación inicial", "Oportunidad recibida desde el servidor.", dto.responsable.orEmpty(), ahora)
                        }
                        local.sincronizado -> oportunidadDao.actualizar(
                            dto.aOportunidad(idLocal = local.id, fotoRuta = local.fotoRuta, actualizado = ahora)
                        )
                    }
                }

                oportunidadDao.listar()
                    .filter { it.sincronizado && it.remoteId != null && it.remoteId !in idsRemotos }
                    .forEach { o ->
                        oportunidadDao.desvincular(o.id)
                        val actualizada = oportunidadDao.obtener(o.id)
                        if (actualizada != null && !subir(actualizada)) rechazadas++
                    }

                _oportunidades.value = oportunidadDao.listar()
                _sync.value = EstadoSync.Sincronizado(LocalTime.now().format(HORA), rechazadas)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _sync.value = EstadoSync.SinConexion("No se pudo conectar con el servidor. Los cambios quedan guardados en el teléfono.")
                false
            } catch (e: HttpException) {
                _sync.value = EstadoSync.SinConexion("El servidor respondió con error ${e.code()}. Se reintentará más tarde.")
                false
            } catch (e: Exception) {
                _sync.value = EstadoSync.SinConexion("Error al sincronizar: ${e.message ?: e.javaClass.simpleName}")
                false
            }
        }
    }

    private suspend fun subir(o: Oportunidad): Boolean {
        val dto = o.aDto()
        val remota = try {
            if (o.remoteId == null) api.crear(dto) else api.actualizar(o.remoteId, dto)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> api.crear(dto.copy(id = null))
                400 -> return false
                else -> throw e
            }
        }
        val remoteId = remota.id ?: return true
        val sinCambiosNuevos = oportunidadDao.obtener(o.id)?.aDto()?.copy(id = null) == dto.copy(id = null)
        oportunidadDao.marcarSincronizada(o.id, remoteId, sinCambiosNuevos)
        return true
    }

    private companion object {
        val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    private fun registrarAccion(id: Long, tipo: String, resultado: String, responsable: String, ahora: String) {
        seguimientoDao.insertar(
            Seguimiento(
                oportunidadId = id, tipoAccion = tipo, fecha = Fechas.aIso(LocalDate.now()),
                resultado = resultado, proximaAccion = "", fechaProximo = null,
                responsable = responsable, registradoEn = ahora
            )
        )
    }
}
