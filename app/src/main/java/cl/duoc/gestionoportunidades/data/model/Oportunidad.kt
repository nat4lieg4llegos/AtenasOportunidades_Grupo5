package cl.duoc.gestionoportunidades.data.model

import java.time.LocalDate

data class Oportunidad(
    val id: Long = 0,
    val nombre: String,
    val organizacion: String,
    val tipo: String,
    val descripcion: String,
    val fuente: String,
    val fechaDeteccion: String,
    val fechaEvento: String,
    val horario: String,
    val comuna: String,
    val region: String,
    val publicoObjetivo: String,
    val publicoEsperado: Int,
    val prioridad: String,
    val potencial: String,
    val estado: String,
    val responsable: String,
    val contactoNombre: String,
    val contactoCargo: String,
    val contactoArea: String,
    val contactoCorreo: String,
    val contactoTelefono: String,
    val propuestaEnviada: Boolean,
    val condiciones: String,
    val costoReferencial: Long,
    val observaciones: String,
    val fotoRuta: String? = null,
    val proximoSeguimiento: String? = null,
    val actualizado: String = "",
    val remoteId: Long? = null,
    val sincronizado: Boolean = false
) {
    val fechaEventoLocal: LocalDate? get() = Fechas.desdeIso(fechaEvento)
    val proximoSeguimientoLocal: LocalDate? get() = proximoSeguimiento?.let { Fechas.desdeIso(it) }
    val estaActiva: Boolean get() = estado in Catalogos.ESTADOS_ACTIVOS

    fun seguimientoAtrasado(hoy: LocalDate): Boolean =
        estaActiva && proximoSeguimientoLocal?.isBefore(hoy) == true

    fun eventoProximo(hoy: LocalDate, dias: Long = 30): Boolean {
        val fecha = fechaEventoLocal ?: return false
        return estaActiva && !fecha.isBefore(hoy) && !fecha.isAfter(hoy.plusDays(dias))
    }

    val sinContacto: Boolean get() = estado in Catalogos.ESTADOS_SIN_CONTACTO
}
