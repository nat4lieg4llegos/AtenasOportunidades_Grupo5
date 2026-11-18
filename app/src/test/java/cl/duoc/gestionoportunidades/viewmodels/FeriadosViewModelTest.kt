package cl.duoc.gestionoportunidades.viewmodels

import cl.duoc.gestionoportunidades.data.model.Feriado
import cl.duoc.gestionoportunidades.repository.FeriadoRepository
import cl.duoc.gestionoportunidades.util.ReglaDispatcherPrincipal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeriadosViewModelTest {

    @get:Rule
    val regla = ReglaDispatcherPrincipal()

    private val repo: FeriadoRepository = mockk()
    private val hoy = LocalDate.of(2026, 11, 18)
    private val feriados2026 = listOf(
        Feriado(LocalDate.of(2026, 9, 18), "Independencia Nacional", true),
        Feriado(LocalDate.of(2026, 12, 8), "Inmaculada Concepción", true)
    )

    @Test
    fun alIniciar_cargaElAnioActualYDestacaElProximoFeriado() {
        coEvery { repo.feriados(2026) } returns feriados2026

        val vm = FeriadosViewModel(repo) { hoy }

        val estado = vm.estado.value
        assertEquals(2026, estado.anio)
        assertEquals(2, estado.feriados.size)
        assertEquals("Inmaculada Concepción", estado.proximo?.nombre)
        assertFalse(estado.cargando)
        assertNull(estado.error)
    }

    @Test
    fun sinConexion_muestraMensajeDeError() {
        coEvery { repo.feriados(2026) } throws IOException("sin red")

        val vm = FeriadosViewModel(repo) { hoy }

        val estado = vm.estado.value
        assertEquals(FeriadosViewModel.MENSAJE_ERROR, estado.error)
        assertTrue(estado.feriados.isEmpty())
        assertFalse(estado.cargando)
    }

    @Test
    fun reintentar_vuelveAConsultarYLimpiaElError() {
        coEvery { repo.feriados(2026) } throws IOException("sin red")
        val vm = FeriadosViewModel(repo) { hoy }
        coEvery { repo.feriados(2026) } returns feriados2026

        vm.reintentar()

        assertNull(vm.estado.value.error)
        assertEquals(2, vm.estado.value.feriados.size)
    }

    @Test
    fun cambiarDeAnio_consultaElAnioSiguienteYAnterior() {
        coEvery { repo.feriados(any()) } returns emptyList()
        val vm = FeriadosViewModel(repo) { hoy }

        vm.anioSiguiente()
        assertEquals(2027, vm.estado.value.anio)
        vm.anioAnterior()
        vm.anioAnterior()
        assertEquals(2025, vm.estado.value.anio)

        coVerify { repo.feriados(2027) }
        coVerify { repo.feriados(2025) }
    }
}
