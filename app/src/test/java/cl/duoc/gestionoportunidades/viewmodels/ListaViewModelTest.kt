package cl.duoc.gestionoportunidades.viewmodels

import android.app.Application
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.repository.OportunidadRepository
import cl.duoc.gestionoportunidades.util.ReglaDispatcherPrincipal
import cl.duoc.gestionoportunidades.util.oportunidadDePrueba
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListaViewModelTest {

    @get:Rule
    val regla = ReglaDispatcherPrincipal()

    private val app: Application = mockk(relaxed = true)
    private val repo: OportunidadRepository = mockk(relaxed = true)
    private val hoy = LocalDate.now()

    private val feria = oportunidadDePrueba(id = 1, nombre = "Feria laboral", prioridad = "Baja", fechaEvento = hoy.plusDays(5))
    private val jornada = oportunidadDePrueba(
        id = 2, nombre = "Jornada municipal", estado = "Propuesta enviada", prioridad = "Alta",
        tipo = "Actividad municipal", region = "Valparaíso", comuna = "Viña del Mar", fechaEvento = hoy.plusDays(90)
    )
    private val encuentro = oportunidadDePrueba(id = 3, nombre = "Encuentro de proveedores", prioridad = "Media", fechaEvento = hoy.plusDays(20))
    private val todas = listOf(feria, jornada, encuentro)
    private val lista = MutableStateFlow<List<Oportunidad>>(todas)

    @Before
    fun preparar() {
        every { repo.oportunidades } returns lista
    }

    @Test
    fun buscarPorTexto_ignoraMayusculasYBuscaEnComuna() {
        assertEquals(listOf(jornada), ListaViewModel.aplicar(todas, Filtros(texto = "viña"), hoy))
        assertEquals(listOf(feria), ListaViewModel.aplicar(todas, Filtros(texto = "FERIA"), hoy))
    }

    @Test
    fun filtrosDeEstadoTipoYRegion_seCombinan() {
        val filtros = Filtros(estado = "Propuesta enviada", tipo = "Actividad municipal", region = "Valparaíso")
        assertEquals(listOf(jornada), ListaViewModel.aplicar(todas, filtros, hoy))
        assertEquals(emptyList<Oportunidad>(), ListaViewModel.aplicar(todas, filtros.copy(region = "Maule"), hoy))
    }

    @Test
    fun soloProximos_dejaLosEventosDeLos30Dias() {
        val resultado = ListaViewModel.aplicar(todas, Filtros(soloProximos = true), hoy)
        assertEquals(listOf(feria, encuentro), resultado)
    }

    @Test
    fun ordenamientos_porFechaPrioridadYRecientes() {
        assertEquals(listOf(feria, encuentro, jornada), ListaViewModel.aplicar(todas, Filtros(orden = Orden.FECHA), hoy))
        assertEquals(listOf(jornada, encuentro, feria), ListaViewModel.aplicar(todas, Filtros(orden = Orden.PRIORIDAD), hoy))
        assertEquals(listOf(encuentro, jornada, feria), ListaViewModel.aplicar(todas, Filtros(orden = Orden.RECIENTES), hoy))
    }

    @Test
    fun elViewModelAplicaLosFiltrosElegidos() = runTest(regla.dispatcher) {
        val vm = ListaViewModel(app, repo)
        backgroundScope.launch { vm.estado.collect {} }

        vm.onPrioridad("Alta")

        assertEquals(listOf(jornada), vm.estado.value.resultados)
        assertEquals(3, vm.estado.value.total)
        coVerify { repo.refrescar() }
    }

    @Test
    fun tocarDosVecesElMismoFiltro_loDesactiva() = runTest(regla.dispatcher) {
        val vm = ListaViewModel(app, repo)
        backgroundScope.launch { vm.estado.collect {} }

        vm.onEstado("Propuesta enviada")
        assertEquals(1, vm.estado.value.filtros.activos)
        vm.onEstado("Propuesta enviada")

        assertNull(vm.estado.value.filtros.estado)
        assertEquals(3, vm.estado.value.resultados.size)
    }

    @Test
    fun limpiar_quitaFiltrosPeroMantieneTextoYOrden() = runTest(regla.dispatcher) {
        val vm = ListaViewModel(app, repo)
        backgroundScope.launch { vm.estado.collect {} }
        vm.onTexto("feria")
        vm.onOrden(Orden.PRIORIDAD)
        vm.onTipo("Feria empresarial")
        vm.onRegion("Metropolitana")
        vm.onSoloProximos()
        assertEquals(3, vm.estado.value.filtros.activos)

        vm.limpiar()

        val filtros = vm.estado.value.filtros
        assertEquals(0, filtros.activos)
        assertEquals("feria", filtros.texto)
        assertEquals(Orden.PRIORIDAD, filtros.orden)
    }
}
