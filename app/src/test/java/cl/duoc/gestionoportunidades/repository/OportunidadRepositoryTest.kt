package cl.duoc.gestionoportunidades.repository

import cl.duoc.gestionoportunidades.data.local.OportunidadDao
import cl.duoc.gestionoportunidades.data.local.SeguimientoDao
import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.data.model.Seguimiento
import cl.duoc.gestionoportunidades.data.remote.OportunidadApiService
import cl.duoc.gestionoportunidades.data.remote.OportunidadDto
import cl.duoc.gestionoportunidades.data.remote.aDto
import cl.duoc.gestionoportunidades.util.oportunidadDePrueba
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OportunidadRepositoryTest {

    private val dao: OportunidadDao = mockk(relaxed = true)
    private val seguimientoDao: SeguimientoDao = mockk(relaxed = true)
    private val api: OportunidadApiService = mockk(relaxed = true)

    private fun TestScope.crearRepositorio() =
        OportunidadRepository(dao, seguimientoDao, api, StandardTestDispatcher(testScheduler))

    private fun errorHttp(codigo: Int) = HttpException(Response.error<Any>(codigo, "error".toResponseBody(null)))

    @Before
    fun preparar() {
        every { dao.listar() } returns emptyList()
        every { dao.listarPendientes() } returns emptyList()
        every { dao.listarEliminacionesPendientes() } returns emptyList()
        coEvery { api.listar() } returns emptyList()
    }

    @Test
    fun refrescar_emiteLaListaGuardada() = runTest {
        val repo = crearRepositorio()
        every { dao.listar() } returns listOf(oportunidadDePrueba(id = 1), oportunidadDePrueba(id = 2))

        repo.refrescar()

        assertEquals(2, repo.oportunidades.value.size)
    }

    @Test
    fun obtenerYSeguimientos_consultanLosDao() = runTest {
        val repo = crearRepositorio()
        every { dao.obtener(1) } returns oportunidadDePrueba(id = 1)
        every { seguimientoDao.listarPorOportunidad(1) } returns emptyList()

        assertEquals(1L, repo.obtener(1)?.id)
        assertTrue(repo.seguimientos(1).isEmpty())
        verify { seguimientoDao.listarPorOportunidad(1) }
    }

    @Test
    fun guardarNueva_insertaPendienteDeSincronizarYRegistraHistorial() = runTest {
        val repo = crearRepositorio()
        every { dao.insertar(any()) } returns 7L

        val id = repo.guardar(oportunidadDePrueba(id = 0), responsable = "Ejecutiva Demo")

        assertEquals(7L, id)
        verify { dao.insertar(match { !it.sincronizado && it.actualizado.isNotBlank() }) }
        verify { seguimientoDao.insertar(match { it.oportunidadId == 7L && it.tipoAccion == "Investigación inicial" }) }
    }

    @Test
    fun guardarExistente_conservaIdDelServidorYRegistraCambioDeEstado() = runTest {
        val repo = crearRepositorio()
        every { dao.obtener(1) } returns oportunidadDePrueba(id = 1, estado = "Evento identificado", remoteId = 5)

        repo.guardar(oportunidadDePrueba(id = 1, estado = "Contacto realizado", remoteId = null), "Ejecutiva Demo")

        verify { dao.actualizar(match { it.remoteId == 5L && it.estado == "Contacto realizado" && !it.sincronizado }) }
        verify {
            seguimientoDao.insertar(match {
                it.tipoAccion == Catalogos.ACCION_CAMBIO_ESTADO && it.resultado == "Evento identificado → Contacto realizado"
            })
        }
    }

    @Test
    fun cambiarEstado_alMismoEstadoNoGuardaNada() = runTest {
        val repo = crearRepositorio()
        val o = oportunidadDePrueba(estado = "Contacto realizado")

        repo.cambiarEstado(o, "Contacto realizado", "Ejecutiva Demo")

        verify(exactly = 0) { dao.actualizar(any()) }
    }

    @Test
    fun cambiarEstado_aPropuestaEnviadaMarcaLaPropuesta() = runTest {
        val repo = crearRepositorio()
        val o = oportunidadDePrueba(id = 1, estado = "Contacto realizado")
        every { dao.obtener(1) } returns o

        repo.cambiarEstado(o, "Propuesta enviada", "Ejecutiva Demo")

        verify { dao.actualizar(match { it.estado == "Propuesta enviada" && it.propuestaEnviada }) }
    }

    @Test
    fun eliminar_dejaPendienteElBorradoEnElServidor() = runTest {
        val repo = crearRepositorio()

        repo.eliminar(oportunidadDePrueba(id = 1, remoteId = 9))

        verify { dao.agregarEliminacionPendiente(9) }
        verify { dao.eliminar(1) }
    }

    @Test
    fun primerContacto_cambiaElEstadoAContactoRealizado() = runTest {
        val repo = crearRepositorio()
        every { dao.obtener(1) } returns oportunidadDePrueba(id = 1, estado = "Contacto pendiente")

        repo.agregarSeguimiento(
            Seguimiento(
                oportunidadId = 1, tipoAccion = "Primer contacto", fecha = "2026-11-18",
                resultado = "Se envió correo de presentación", proximaAccion = "Llamar",
                fechaProximo = "2026-12-01", responsable = "Ejecutiva Demo"
            )
        )

        verify { dao.actualizar(match { it.estado == "Contacto realizado" && it.proximoSeguimiento == "2026-12-01" }) }
        verify(exactly = 2) { seguimientoDao.insertar(any()) }
    }

    @Test
    fun envioDePropuesta_marcaLaPropuestaSinCambiarEstado() = runTest {
        val repo = crearRepositorio()
        every { dao.obtener(1) } returns oportunidadDePrueba(id = 1, estado = "Contacto realizado")

        repo.agregarSeguimiento(
            Seguimiento(
                oportunidadId = 1, tipoAccion = "Envío de propuesta", fecha = "2026-11-18",
                resultado = "Propuesta enviada por correo", proximaAccion = "", fechaProximo = null,
                responsable = "Ejecutiva Demo"
            )
        )

        verify { dao.actualizar(match { it.estado == "Contacto realizado" && it.propuestaEnviada }) }
        verify(exactly = 1) { seguimientoDao.insertar(any()) }
    }

    @Test
    fun sincronizar_subeOportunidadesNuevasConPost() = runTest {
        val repo = crearRepositorio()
        val local = oportunidadDePrueba(id = 1, remoteId = null)
        every { dao.listarPendientes() } returns listOf(local)
        every { dao.obtener(1) } returns local
        coEvery { api.crear(any()) } returns local.aDto().copy(id = 50)

        val resultado = repo.sincronizar()

        assertTrue(resultado)
        coVerify { api.crear(match { it.id == null && it.nombre == local.nombre }) }
        verify { dao.marcarSincronizada(1, 50, true) }
        assertTrue(repo.sync.value is EstadoSync.Sincronizado)
    }

    @Test
    fun sincronizar_actualizaConPutLasQueYaEstanEnElServidor() = runTest {
        val repo = crearRepositorio()
        val local = oportunidadDePrueba(id = 1, remoteId = 5)
        every { dao.listarPendientes() } returns listOf(local)
        every { dao.obtener(1) } returns local
        coEvery { api.actualizar(5, any()) } returns local.aDto()

        repo.sincronizar()

        coVerify { api.actualizar(5, any()) }
        verify { dao.marcarSincronizada(1, 5, true) }
    }

    @Test
    fun sincronizar_siElServidorYaNoLaTieneLaVuelveACrear() = runTest {
        val repo = crearRepositorio()
        val local = oportunidadDePrueba(id = 1, remoteId = 5)
        every { dao.listarPendientes() } returns listOf(local)
        every { dao.obtener(1) } returns local
        coEvery { api.actualizar(5, any()) } throws errorHttp(404)
        coEvery { api.crear(any()) } returns local.aDto().copy(id = 60)

        repo.sincronizar()

        verify { dao.marcarSincronizada(1, 60, any()) }
    }

    @Test
    fun sincronizar_cuentaLasRechazadasPorElServidor() = runTest {
        val repo = crearRepositorio()
        every { dao.listarPendientes() } returns listOf(oportunidadDePrueba(id = 1))
        coEvery { api.crear(any()) } throws errorHttp(400)

        repo.sincronizar()

        assertEquals(1, (repo.sync.value as EstadoSync.Sincronizado).rechazadas)
    }

    @Test
    fun sincronizar_descargaLasOportunidadesCreadasEnOtroTelefono() = runTest {
        val repo = crearRepositorio()
        coEvery { api.listar() } returns listOf(
            OportunidadDto(id = 77, nombre = "Feria remota", fechaEvento = "2026-12-01", responsable = "Supervisor Demo")
        )
        every { dao.obtenerPorRemoteId(77) } returns null
        every { dao.insertar(any()) } returns 3L

        repo.sincronizar()

        verify { dao.insertar(match { it.remoteId == 77L && it.sincronizado && it.nombre == "Feria remota" }) }
        verify { seguimientoDao.insertar(match { it.oportunidadId == 3L }) }
    }

    @Test
    fun sincronizar_actualizaLaCopiaLocalConservandoLaFoto() = runTest {
        val repo = crearRepositorio()
        coEvery { api.listar() } returns listOf(OportunidadDto(id = 77, nombre = "Nombre nuevo"))
        every { dao.obtenerPorRemoteId(77) } returns
            oportunidadDePrueba(id = 4, remoteId = 77, sincronizado = true, fotoRuta = "/fotos/evento.jpg")

        repo.sincronizar()

        verify { dao.actualizar(match { it.id == 4L && it.nombre == "Nombre nuevo" && it.fotoRuta == "/fotos/evento.jpg" }) }
    }

    @Test
    fun sincronizar_enviaLosBorradosPendientes() = runTest {
        val repo = crearRepositorio()
        every { dao.listarEliminacionesPendientes() } returns listOf(9L, 10L)
        coEvery { api.eliminar(9) } returns Response.success(Unit)
        coEvery { api.eliminar(10) } returns Response.error<Unit>(404, "".toResponseBody(null))

        repo.sincronizar()

        verify { dao.quitarEliminacionPendiente(9) }
        verify { dao.quitarEliminacionPendiente(10) }
    }

    @Test
    fun sincronizar_vuelveASubirLasQueDesaparecieronDelServidor() = runTest {
        val repo = crearRepositorio()
        val sincronizada = oportunidadDePrueba(id = 1, remoteId = 8, sincronizado = true)
        every { dao.listar() } returns listOf(sincronizada)
        every { dao.obtener(1) } returns sincronizada.copy(remoteId = null, sincronizado = false)
        coEvery { api.crear(any()) } returns sincronizada.aDto().copy(id = 81)

        repo.sincronizar()

        verify { dao.desvincular(1) }
        verify { dao.marcarSincronizada(1, 81, any()) }
    }

    @Test
    fun sincronizar_sinInternetAvisaYNoPierdeDatos() = runTest {
        val repo = crearRepositorio()
        coEvery { api.listar() } throws IOException("sin red")

        val resultado = repo.sincronizar()

        assertFalse(resultado)
        assertTrue(repo.sync.value is EstadoSync.SinConexion)
        verify(exactly = 0) { dao.eliminar(any()) }
    }

    @Test
    fun sincronizar_errorDelServidorMuestraElCodigo() = runTest {
        val repo = crearRepositorio()
        coEvery { api.listar() } throws errorHttp(500)

        repo.sincronizar()

        val estado = repo.sync.value as EstadoSync.SinConexion
        assertTrue(estado.detalle.contains("500"))
    }

    @Test
    fun sincronizar_errorInesperadoTambienQuedaInformado() = runTest {
        val repo = crearRepositorio()
        coEvery { api.listar() } throws IllegalStateException("respuesta inválida")

        repo.sincronizar()

        val estado = repo.sync.value as EstadoSync.SinConexion
        assertTrue(estado.detalle.contains("respuesta inválida"))
    }
}
