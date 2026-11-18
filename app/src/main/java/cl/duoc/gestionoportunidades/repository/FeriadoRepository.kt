package cl.duoc.gestionoportunidades.repository

import cl.duoc.gestionoportunidades.data.model.Feriado
import cl.duoc.gestionoportunidades.data.remote.FeriadoDto
import cl.duoc.gestionoportunidades.data.remote.FeriadosApiService
import java.time.LocalDate

class FeriadoRepository(private val api: FeriadosApiService) {

    suspend fun feriados(anio: Int): List<Feriado> = aFeriados(api.feriados(anio, PAIS))

    companion object {
        const val PAIS = "CL"

        fun aFeriados(lista: List<FeriadoDto>): List<Feriado> =
            lista.mapNotNull { dto ->
                val fecha = dto.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@mapNotNull null
                val nombre = dto.localName?.takeIf { it.isNotBlank() } ?: dto.name.orEmpty()
                Feriado(fecha = fecha, nombre = nombre, nacional = dto.global ?: true)
            }
                .distinctBy { it.fecha to it.nombre }
                .sortedBy { it.fecha }

        fun proximo(lista: List<Feriado>, hoy: LocalDate): Feriado? =
            lista.firstOrNull { !it.fecha.isBefore(hoy) }
    }
}
