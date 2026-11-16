package cl.duoc.gestionoportunidades.repository

/** Estado de la conexión con el microservicio REST, para mostrarlo en el panel de inicio. */
sealed interface EstadoSync {
    data object Inactivo : EstadoSync
    data object Sincronizando : EstadoSync
    data class Sincronizado(val hora: String, val rechazadas: Int = 0) : EstadoSync
    data class SinConexion(val detalle: String) : EstadoSync
}
