package cl.duoc.gestionoportunidades.viewmodels

import android.app.Application
import cl.duoc.gestionoportunidades.data.model.Seguimiento
import cl.duoc.gestionoportunidades.repository.AuthRepository
import cl.duoc.gestionoportunidades.repository.OportunidadRepository
import cl.duoc.gestionoportunidades.util.ReglaDispatcherPrincipal
import cl.duoc.gestionoportunidades.util.USUARIO_DEMO
import cl.duoc.gestionoportunidades.util.oportunidadDePrueba
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DetalleViewModelTest {

    @get:Rule
    val regla = ReglaDispatcherPrincipal()

    private val app: Application = mockk(relaxed = true)
    private val repo: OportunidadRepository = mockk(relaxed = true)
    private val auth: AuthRepository = mockk()
    private val oportunidad = oportunidadDePrueba(id = 1, estado = "Contacto pendiente")
    private val seguimiento = Seguimiento(
        id = 1, oportunidadId = 1, tipoAccion = "Llamada", fecha = "2026-11-18",
        resultado = "Sin respuesta", proximaAccion = "", fechaProximo = null, responsable = "Ejecutiva Demo"
    )

    @Before
    fun preparar() {
        every { repo.oportunidades } returns MutableStateFlow(listOf(oportunidad))
        coEvery { repo.seguimientos(1) } returns listOf(seguimiento)
        every { auth.usuarioActual() } returns USUARIO_DEMO
    }

    @Test
    fun cargar_muestraLaOportunidadYSuHistorial() {
        val vm = DetalleViewModel(app, repo, auth)

        vm.cargar(1)

        assertEquals(oportunidad, vm.estado.value.oportunidad)
        assertEquals(listOf(seguimiento), vm.estado.value.seguimientos)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun cargarUnaQueNoExiste_dejaLaFichaVacia() {
        coEvery { repo.obtener(99) } returns null
        val vm = DetalleViewModel(app, repo, auth)

        vm.cargar(99)

        assertNull(vm.estado.value.oportunidad)
        assertTrue(vm.estado.value.seguimientos.isEmpty())
        coVerify(exactly = 0) { repo.seguimientos(99) }
    }

    @Test
    fun cambiarEstado_guardaConElUsuarioActualYAvisa() {
        val vm = DetalleViewModel(app, repo, auth)
        vm.cargar(1)

        vm.cambiarEstado("Contacto realizado")

        coVerify { repo.cambiarEstado(oportunidad, "Contacto realizado", "Ejecutiva Demo") }
        assertEquals("Estado actualizado a \"Contacto realizado\"", vm.estado.value.mensaje)
    }

    @Test
    fun sinOportunidadCargada_noSePuedeCambiarNiEliminar() {
        val vm = DetalleViewModel(app, repo, auth)

        vm.cambiarEstado("Contacto realizado")
        vm.eliminar()

        coVerify(exactly = 0) { repo.cambiarEstado(any(), any(), any()) }
        coVerify(exactly = 0) { repo.eliminar(any()) }
    }

    @Test
    fun eliminar_marcaLaFichaComoEliminada() {
        val vm = DetalleViewModel(app, repo, auth)
        vm.cargar(1)

        vm.eliminar()

        assertTrue(vm.estado.value.eliminada)
        coVerify { repo.eliminar(oportunidad) }
    }

    @Test
    fun mensajes_seMuestranYSeLimpian() {
        val vm = DetalleViewModel(app, repo, auth)

        vm.permisoNotificacionDenegado()
        assertTrue(vm.estado.value.mensaje!!.startsWith("Sin permiso de notificaciones"))
        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }
}
