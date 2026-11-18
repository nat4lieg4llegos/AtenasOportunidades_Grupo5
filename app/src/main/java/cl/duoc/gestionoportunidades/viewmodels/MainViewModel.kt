package cl.duoc.gestionoportunidades.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.gestionoportunidades.navigation.NavigationEvent
import cl.duoc.gestionoportunidades.navigation.Screen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)

    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    fun navigateTo(screen: Screen, popUpTo: Screen? = null, inclusive: Boolean = false, mensaje: String? = null) {
        enviar(NavigationEvent.NavigateTo(route = screen, popUpToRoute = popUpTo, inclusive = inclusive, mensaje = mensaje))
    }

    fun navigateBack(mensaje: String? = null) = enviar(NavigationEvent.PopBackStack(mensaje))

    fun navigateUp() = enviar(NavigationEvent.NavigateUp)

    private fun enviar(evento: NavigationEvent) {
        viewModelScope.launch { _navigationEvents.send(evento) }
    }
}
