package cl.duoc.gestionoportunidades.viewmodels

import cl.duoc.gestionoportunidades.navigation.NavigationEvent
import cl.duoc.gestionoportunidades.navigation.Screen
import cl.duoc.gestionoportunidades.util.ReglaDispatcherPrincipal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val regla = ReglaDispatcherPrincipal()

    @Test
    fun navigateTo_emiteElEventoConSusOpciones() = runTest(regla.dispatcher) {
        val vm = MainViewModel()

        vm.navigateTo(Screen.Detalle(5), popUpTo = Screen.Formulario(), inclusive = true, mensaje = "Oportunidad registrada")

        assertEquals(
            NavigationEvent.NavigateTo(
                route = Screen.Detalle(5),
                popUpToRoute = Screen.Formulario(),
                inclusive = true,
                mensaje = "Oportunidad registrada"
            ),
            vm.navigationEvents.first()
        )
    }

    @Test
    fun navigateBackYNavigateUp_emitenSusEventos() = runTest(regla.dispatcher) {
        val vm = MainViewModel()

        vm.navigateBack(mensaje = "Cambios guardados")
        vm.navigateUp()

        assertEquals(NavigationEvent.PopBackStack("Cambios guardados"), vm.navigationEvents.first())
        assertEquals(NavigationEvent.NavigateUp, vm.navigationEvents.first())
    }

    @Test
    fun lasRutasReemplazanElId() {
        assertEquals("login", Screen.Login.buildRoute())
        assertEquals("formulario/0", Screen.Formulario().buildRoute())
        assertEquals("detalle/5", Screen.Detalle(5).buildRoute())
        assertEquals("seguimiento/3", Screen.NuevoSeguimiento(3).buildRoute())
        assertEquals("feriados", Screen.Feriados.route)
    }
}
