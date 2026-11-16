package cl.duoc.gestionoportunidades.navigation

/** Eventos de navegación que emite MainViewModel y que AppNavigation ejecuta con el NavController. */
sealed class NavigationEvent {

    /**
     * Navegar a una pantalla.
     * @param route pantalla de destino (objeto Screen, más seguro que un texto).
     * @param popUpToRoute si no es null, se sacan de la pila las pantallas hasta esta.
     * @param inclusive si es true, también se saca [popUpToRoute].
     * @param singleTop si es true, no se repite la pantalla si ya está arriba de la pila.
     * @param mensaje texto opcional que la pantalla de destino muestra en un Snackbar.
     */
    data class NavigateTo(
        val route: Screen,
        val popUpToRoute: Screen? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false,
        val mensaje: String? = null
    ) : NavigationEvent()

    /** Volver a la pantalla anterior; opcionalmente le deja un mensaje (ej. "Cambios guardados"). */
    data class PopBackStack(val mensaje: String? = null) : NavigationEvent()

    /** Subir en la jerarquía (flecha "atrás" de la barra superior). */
    data object NavigateUp : NavigationEvent()
}
