package cl.duoc.gestionoportunidades.repository

sealed interface EstadoSync {
    data object Inactivo : EstadoSync
    data object Sincronizando : EstadoSync
    data class Sincronizado(val hora: String, val rechazadas: Int = 0) : EstadoSync
    data class SinConexion(val detalle: String) : EstadoSync
}
