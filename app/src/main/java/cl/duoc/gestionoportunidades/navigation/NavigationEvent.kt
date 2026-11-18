package cl.duoc.gestionoportunidades.navigation

sealed class NavigationEvent {
    data class NavigateTo(
        val route: Screen,
        val popUpToRoute: Screen? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false,
        val mensaje: String? = null
    ) : NavigationEvent()

    data class PopBackStack(val mensaje: String? = null) : NavigationEvent()

    data object NavigateUp : NavigationEvent()
}
