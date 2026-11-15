package cl.duoc.gestionoportunidades.data.repository

import cl.duoc.gestionoportunidades.data.local.OportunidadDao
import cl.duoc.gestionoportunidades.data.local.SeguimientoDao
import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.data.model.Seguimiento
import cl.duoc.gestionoportunidades.data.remote.OportunidadApiService
import cl.duoc.gestionoportunidades.data.remote.aDto
import cl.duoc.gestionoportunidades.data.remote.aOportunidad
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
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Estado de la conexión con el microservicio, para mostrarlo en el panel. */
sealed interface EstadoSync {
    data object Inactivo : EstadoSync
    data object Sincronizando : EstadoSync
    data class Sincronizado(val hora: String, val rechazadas: Int = 0) : EstadoSync
    data class SinConexion(val detalle: String) : EstadoSync
}

/**
 * Única fuente de datos para los ViewModels. Expone la lista como StateFlow y la
 * vuelve a emitir después de cada escritura, para que todas las pantallas se actualicen solas.
 *
 * EP3: además de SQLite, se conecta al microservicio REST con Retrofit ("offline first"):
 *  1. Toda escritura se guarda primero en el teléfono (la app funciona sin internet).
 *  2. Luego se sincroniza con el servidor en segundo plano (POST / PUT / DELETE).
 *  3. Al sincronizar también se descargan (GET) las oportunidades creadas desde otros teléfonos.
 * Todo el acceso a SQLite y a la red se ejecuta fuera del hilo principal (Dispatchers.IO).
 */
class OportunidadRepository(
    private val oportunidadDao: OportunidadDao,
    private val seguimientoDao: SeguimientoDao,
    private val api: OportunidadApiService
) {
    private val _oportunidades = MutableStateFlow<List<Oportunidad>>(emptyList())
    val oportunidades: StateFlow<List<Oportunidad>> = _oportunidades.asStateFlow()

    private val _sync = MutableStateFlow<EstadoSync>(EstadoSync.Inactivo)
    val sync: StateFlow<EstadoSync> = _sync.asStateFlow()

    /** Alcance propio del repositorio: la sincronización sigue aunque el usuario cambie de pantalla. */
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Evita dos sincronizaciones al mismo tiempo (por ejemplo, guardar dos veces seguidas). */
    private val candado = Mutex()

    suspend fun refrescar() = withContext(Dispatchers.IO) {
        _oportunidades.value = oportunidadDao.listar()
    }

    suspend fun obtener(id: Long): Oportunidad? = withContext(Dispatchers.IO) { oportunidadDao.obtener(id) }

    suspend fun seguimientos(oportunidadId: Long): List<Seguimiento> =
        withContext(Dispatchers.IO) { seguimientoDao.listarPorOportunidad(oportunidadId) }

    /** Inserta o actualiza. Si cambió el estado, lo deja registrado en el historial. Devuelve el id. */
    suspend fun guardar(o: Oportunidad, responsable: String): Long = withContext(Dispatchers.IO) {
        val ahora = Fechas.marcaTiempo()
        val id = if (o.id == 0L) {
            val nuevoId = oportunidadDao.insertar(o.copy(actualizado = ahora, sincronizado = false))
            registrarAccion(nuevoId, "Investigación inicial", "Oportunidad registrada en la aplicación.", responsable, ahora)
            nuevoId
        } else {
            val anterior = oportunidadDao.obtener(o.id)
            // Se conserva el id del servidor guardado en la base (pudo asignarse mientras el formulario estaba abierto).
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
        // Si ya estaba en el servidor, se anota para borrarla allá también (aunque ahora no haya internet).
        o.remoteId?.let { oportunidadDao.agregarEliminacionPendiente(it) }
        oportunidadDao.eliminar(o.id) // los seguimientos se borran en cascada
        o.fotoRuta?.let { File(it).delete() }
        _oportunidades.value = oportunidadDao.listar()
        sincronizarEnSegundoPlano()
    }

    /** Registra un seguimiento y actualiza la fecha del próximo seguimiento de la oportunidad. */
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

    // ---------------------------------------------------------------------------------------
    // Sincronización con el microservicio REST (Retrofit)
    // ---------------------------------------------------------------------------------------

    fun sincronizarEnSegundoPlano() {
        alcance.launch { sincronizar() }
    }

    /**
     * Sincroniza en 4 pasos y devuelve true si terminó bien. Si no hay conexión, no se pierde nada:
     * los cambios quedan marcados como pendientes y se envían en la próxima sincronización.
     */
    suspend fun sincronizar(): Boolean = withContext(Dispatchers.IO) {
        candado.withLock {
            _sync.value = EstadoSync.Sincronizando
            try {
                // 1) DELETE: oportunidades borradas en el teléfono que siguen en el servidor.
                for (remoteId in oportunidadDao.listarEliminacionesPendientes()) {
                    val respuesta = api.eliminar(remoteId)
                    if (!respuesta.isSuccessful && respuesta.code() != 404) throw HttpException(respuesta)
                    oportunidadDao.quitarEliminacionPendiente(remoteId) // 404 = ya no existía: listo igual
                }

                // 2) POST / PUT: cambios locales pendientes.
                var rechazadas = 0
                for (o in oportunidadDao.listarPendientes()) {
                    if (!subir(o)) rechazadas++
                }

                // 3) GET: traer lo que hay en el servidor (incluye lo creado desde otros teléfonos).
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
                        // Solo se sobrescribe si no hay cambios locales sin enviar.
                        local.sincronizado -> oportunidadDao.actualizar(
                            dto.aOportunidad(idLocal = local.id, fotoRuta = local.fotoRuta, actualizado = ahora)
                        )
                    }
                }

                // 4) Si una oportunidad sincronizada ya no está en el servidor, se vuelve a subir
                //    (preferimos no borrar datos del teléfono por un problema del servidor).
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

    /**
     * Envía una oportunidad: POST si nunca se subió, PUT si ya tiene id del servidor.
     * Devuelve false si el servidor la rechazó por datos inválidos (400); los errores de red se propagan.
     */
    private suspend fun subir(o: Oportunidad): Boolean {
        val dto = o.aDto()
        val remota = try {
            if (o.remoteId == null) api.crear(dto) else api.actualizar(o.remoteId, dto)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> api.crear(dto.copy(id = null)) // la borraron en el servidor: se crea de nuevo
                400 -> return false                   // datos rechazados por el servidor
                else -> throw e
            }
        }
        val remoteId = remota.id ?: return true
        // Si el usuario la editó otra vez mientras se enviaba, queda pendiente para la próxima vuelta.
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
