package cl.duoc.gestionoportunidades.navigation

sealed class Screen(val route: String) {
    open fun buildRoute(): String = route

    data object Login : Screen("login")

    data object Inicio : Screen("inicio")

    data object Lista : Screen("lista")

    data class Formulario(val id: Long = 0L) : Screen(RUTA_FORMULARIO) {
        override fun buildRoute(): String = route.replace("{$ARG_ID}", id.toString())
    }

    data class Detalle(val id: Long) : Screen(RUTA_DETALLE) {
        override fun buildRoute(): String = route.replace("{$ARG_ID}", id.toString())
    }

    data class NuevoSeguimiento(val oportunidadId: Long) : Screen(RUTA_SEGUIMIENTO) {
        override fun buildRoute(): String = route.replace("{$ARG_ID}", oportunidadId.toString())
    }

    companion object {
        const val ARG_ID = "id"

        const val CLAVE_MENSAJE = "mensaje"

        const val RUTA_FORMULARIO = "formulario/{id}"
        const val RUTA_DETALLE = "detalle/{id}"
        const val RUTA_SEGUIMIENTO = "seguimiento/{id}"
    }
}
