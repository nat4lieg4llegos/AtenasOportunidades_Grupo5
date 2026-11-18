package cl.duoc.gestionoportunidades.data.model

import java.time.LocalDate

data class Feriado(
    val fecha: LocalDate,
    val nombre: String,
    val nacional: Boolean
)
