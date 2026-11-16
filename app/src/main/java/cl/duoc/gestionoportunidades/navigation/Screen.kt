package cl.duoc.gestionoportunidades.navigation

/**
 * Rutas de la app como sealed class: cada pantalla es un tipo, así no se escriben rutas "a mano"
 * y el compilador avisa si una no existe (rutas tipo-safe).
 * - Pantallas sin argumentos: data object.
 * - Pantallas que reciben el id de una oportunidad: data class con buildRoute().
 */
sealed class Screen(val route: String) {

    /** Ruta final para navegar. Las pantallas sin argumentos usan la ruta tal cual. */
    open fun buildRoute(): String = route

    data object Login : Screen("login")

    data object Inicio : Screen("inicio")

    data object Lista : Screen("lista")

    /** Formulario de oportunidad: id = 0 para crear una nueva, id > 0 para editar. */
    data class Formulario(val id: Long = 0L) : Screen(RUTA_FORMULARIO) {
        override fun buildRoute(): String = route.replace("{$ARG_ID}", id.toString())
    }

    /** Ficha de una oportunidad. */
    data class Detalle(val id: Long) : Screen(RUTA_DETALLE) {
        override fun buildRoute(): String = route.replace("{$ARG_ID}", id.toString())
    }

    /** Registro de un seguimiento para la oportunidad indicada. */
    data class NuevoSeguimiento(val oportunidadId: Long) : Screen(RUTA_SEGUIMIENTO) {
        override fun buildRoute(): String = route.replace("{$ARG_ID}", oportunidadId.toString())
    }

    companion object {
        /** Nombre del argumento que lleva el id en las rutas. */
        const val ARG_ID = "id"

        /** Clave para dejar un mensaje (Snackbar) a la pantalla de destino. */
        const val CLAVE_MENSAJE = "mensaje"

        const val RUTA_FORMULARIO = "formulario/{id}"
        const val RUTA_DETALLE = "detalle/{id}"
        const val RUTA_SEGUIMIENTO = "seguimiento/{id}"
    }
}
