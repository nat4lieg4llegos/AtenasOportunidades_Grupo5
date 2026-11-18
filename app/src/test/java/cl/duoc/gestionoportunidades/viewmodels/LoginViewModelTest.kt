package cl.duoc.gestionoportunidades.viewmodels

import android.app.Application
import cl.duoc.gestionoportunidades.repository.AuthRepository
import cl.duoc.gestionoportunidades.util.ReglaDispatcherPrincipal
import cl.duoc.gestionoportunidades.util.USUARIO_DEMO
import cl.duoc.gestionoportunidades.validacion.CampoLogin
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val regla = ReglaDispatcherPrincipal()

    private val app: Application = mockk(relaxed = true)
    private val auth: AuthRepository = mockk()

    @Test
    fun camposVacios_marcanErrorPorCampoSinConsultarCredenciales() {
        val vm = LoginViewModel(app, auth)

        vm.ingresar()

        val estado = vm.estado.value
        assertEquals(setOf(CampoLogin.USUARIO, CampoLogin.CONTRASENA), estado.errores.keys)
        assertEquals(1, estado.intentosFallidos)
        verify(exactly = 0) { auth.iniciarSesion(any(), any()) }
    }

    @Test
    fun credencialesCorrectas_autenticanAlUsuario() = runTest(regla.dispatcher) {
        every { auth.iniciarSesion("ejecutivo", "Atenas2026") } returns USUARIO_DEMO
        val vm = LoginViewModel(app, auth)
        vm.onUsuario("ejecutivo")
        vm.onContrasena("Atenas2026")

        vm.ingresar()
        assertTrue(vm.estado.value.cargando)
        advanceUntilIdle()

        assertTrue(vm.estado.value.autenticado)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun credencialesIncorrectas_muestranErrorGeneral() = runTest(regla.dispatcher) {
        every { auth.iniciarSesion(any(), any()) } returns null
        val vm = LoginViewModel(app, auth)
        vm.onUsuario("ejecutivo")
        vm.onContrasena("OtraClave99")

        vm.ingresar()
        advanceUntilIdle()

        assertEquals("Usuario o contraseña incorrectos", vm.estado.value.errorGeneral)
        assertEquals(1, vm.estado.value.intentosFallidos)
        assertFalse(vm.estado.value.autenticado)
    }

    @Test
    fun escribirEnUnCampo_quitaSoloElErrorDeEseCampo() {
        val vm = LoginViewModel(app, auth)
        vm.ingresar()

        vm.onUsuario("ejecutivo")

        assertNull(vm.estado.value.errores[CampoLogin.USUARIO])
        assertTrue(CampoLogin.CONTRASENA in vm.estado.value.errores)
    }

    @Test
    fun alternarVisibilidad_muestraYOcultaLaContrasena() {
        val vm = LoginViewModel(app, auth)

        vm.alternarVisibilidad()
        assertTrue(vm.estado.value.mostrarContrasena)
        vm.alternarVisibilidad()
        assertFalse(vm.estado.value.mostrarContrasena)
    }
}
