package cl.duoc.gestionoportunidades.repository

import cl.duoc.gestionoportunidades.data.model.Feriado
import cl.duoc.gestionoportunidades.data.remote.FeriadoDto
import cl.duoc.gestionoportunidades.data.remote.FeriadosApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class FeriadoRepositoryTest {

    private val api: FeriadosApiService = mockk()
    private val repo = FeriadoRepository(api)

    @Test
    fun feriados_consultaLaApiDeChileYOrdenaPorFecha() = runTest {
        coEvery { api.feriados(2026, "CL") } returns listOf(
            FeriadoDto(date = "2026-09-18", localName = "Independencia Nacional"),
            FeriadoDto(date = "2026-01-01", localName = "Año Nuevo")
        )

        val resultado = repo.feriados(2026)

        assertEquals(listOf("Año Nuevo", "Independencia Nacional"), resultado.map { it.nombre })
        assertEquals(LocalDate.of(2026, 1, 1), resultado.first().fecha)
        coVerify(exactly = 1) { api.feriados(2026, "CL") }
    }

    @Test
    fun aFeriados_descartaFechasInvalidasYDuplicados() {
        val resultado = FeriadoRepository.aFeriados(
            listOf(
                FeriadoDto(date = "fecha-mala", localName = "Inválido"),
                FeriadoDto(date = null, localName = "Sin fecha"),
                FeriadoDto(date = "2026-05-21", localName = "Glorias Navales"),
                FeriadoDto(date = "2026-05-21", localName = "Glorias Navales")
            )
        )

        assertEquals(1, resultado.size)
        assertEquals("Glorias Navales", resultado.first().nombre)
    }

    @Test
    fun aFeriados_usaNombreEnInglesSiNoHayLocalYMarcaRegionales() {
        val resultado = FeriadoRepository.aFeriados(
            listOf(FeriadoDto(date = "2026-06-07", localName = "", name = "Assault and Capture of Cape Arica", global = false))
        )

        assertEquals("Assault and Capture of Cape Arica", resultado.first().nombre)
        assertFalse(resultado.first().nacional)
    }

    @Test
    fun proximo_devuelveElPrimerFeriadoDesdeHoy() {
        val lista = listOf(
            Feriado(LocalDate.of(2026, 1, 1), "Año Nuevo", true),
            Feriado(LocalDate.of(2026, 12, 8), "Inmaculada Concepción", true),
            Feriado(LocalDate.of(2026, 12, 25), "Navidad", true)
        )

        assertEquals("Inmaculada Concepción", FeriadoRepository.proximo(lista, LocalDate.of(2026, 11, 18))?.nombre)
        assertEquals("Navidad", FeriadoRepository.proximo(lista, LocalDate.of(2026, 12, 25))?.nombre)
        assertNull(FeriadoRepository.proximo(lista, LocalDate.of(2026, 12, 26)))
    }
}
