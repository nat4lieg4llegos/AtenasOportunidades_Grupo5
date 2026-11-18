package cl.duoc.gestionoportunidades.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

object Fechas {
    private val formatoUi: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val formatoIso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val formatoMarca: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")

    fun desdeUi(texto: String): LocalDate? = try {
        LocalDate.parse(texto.trim(), formatoUi)
    } catch (e: DateTimeParseException) {
        null
    }

    fun desdeIso(texto: String): LocalDate? = try {
        LocalDate.parse(texto.trim(), formatoIso)
    } catch (e: DateTimeParseException) {
        null
    }

    fun aUi(fecha: LocalDate): String = fecha.format(formatoUi)
    fun aIso(fecha: LocalDate): String = fecha.format(formatoIso)

    fun isoAUi(iso: String?): String = iso?.let { desdeIso(it)?.let(::aUi) ?: it }.orEmpty()

    fun uiAIso(texto: String): String? = desdeUi(texto)?.let(::aIso)

    fun marcaTiempo(ahora: LocalDateTime = LocalDateTime.now()): String = ahora.format(formatoMarca)
}
