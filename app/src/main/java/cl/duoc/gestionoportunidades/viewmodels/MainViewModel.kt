package cl.duoc.gestionoportunidades.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.gestionoportunidades.navigation.NavigationEvent
import cl.duoc.gestionoportunidades.navigation.Screen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Centraliza la navegación: las pantallas piden "ir a" una Screen y este ViewModel emite el evento.
 * AppNavigation escucha los eventos y los ejecuta con el NavController.
 * Se usa un Channel para que ningún evento se pierda si se emite antes de que la UI lo escuche
 * (por ejemplo, al abrir la app desde una notificación).
 */
class MainViewModel : ViewModel() {

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)

    /** receiveAsFlow(): expone el Channel como Flow de solo lectura para la UI. */
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    /** Navega a [screen]. Opcionalmente limpia la pila hasta [popUpTo] y deja un [mensaje] al destino. */
    fun navigateTo(screen: Screen, popUpTo: Screen? = null, inclusive: Boolean = false, mensaje: String? = null) {
        enviar(NavigationEvent.NavigateTo(route = screen, popUpToRoute = popUpTo, inclusive = inclusive, mensaje = mensaje))
    }

    /** Vuelve a la pantalla anterior, opcionalmente con un mensaje para ella. */
    fun navigateBack(mensaje: String? = null) = enviar(NavigationEvent.PopBackStack(mensaje))

    /** Flecha "atrás" de la barra superior. */
    fun navigateUp() = enviar(NavigationEvent.NavigateUp)

    private fun enviar(evento: NavigationEvent) {
        viewModelScope.launch { _navigationEvents.send(evento) }
    }
}
