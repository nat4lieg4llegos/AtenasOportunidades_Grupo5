package cl.duoc.gestionoportunidades.viewmodels

import android.app.Application
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.repository.AuthRepository
import cl.duoc.gestionoportunidades.repository.EstadoSync
import cl.duoc.gestionoportunidades.repository.OportunidadRepository
import cl.duoc.gestionoportunidades.util.ReglaDispatcherPrincipal
import cl.duoc.gestionoportunidades.util.USUARIO_DEMO
import cl.duoc.gestionoportunidades.util.oportunidadDePrueba
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModelTest {

    @get:Rule
    val regla = ReglaDispatcherPrincipal()

    private val app: Application = mockk(relaxed = true)
    private val repo: OportunidadRepository = mockk(relaxed = true)
    private val auth: AuthRepository = mockk(relaxed = true)
    private val lista = MutableStateFlow<List<Oportunidad>>(emptyList())
    private val sync = MutableStateFlow<EstadoSync>(EstadoSync.Inactivo)
    private val hoy = LocalDate.now()

    @Before
    fun preparar() {
        every { repo.oportunidades } returns lista
        every { repo.sync } returns sync
        every { auth.usuarioActual() } returns USUARIO_DEMO
    }

    @Test
    fun calculaIndicadoresYAlertasDeGestion() = runTest(regla.dispatcher) {
        lista.value = listOf(
            oportunidadDePrueba(id = 1, estado = "Contacto realizado", proximoSeguimiento = hoy.minusDays(3), fechaEvento = hoy.plusDays(10)),
            oportunidadDePrueba(id = 2, estado = "Contacto pendiente", fechaEvento = hoy.plusDays(3)),
            oportunidadDePrueba(id = 3, estado = "Participación confirmada", fechaEvento = hoy.plusDays(60)),
            oportunidadDePrueba(id = 4, estado = "Oportunidad descartada", fechaEvento = hoy.plusDays(2))
        )
        val vm = InicioViewModel(app, repo, auth)
        backgroundScope.launch { vm.estado.collect {} }

        val estado = vm.estado.value
        assertEquals(4, estado.total)
        assertEquals(2, estado.proximos)
        assertEquals(1, estado.atrasados)
        assertEquals(1, estado.confirmadas)
        assertEquals(listOf(TipoAlerta.ATRASADO, TipoAlerta.PROXIMO, TipoAlerta.SIN_CONTACTO), estado.alertas.map { it.tipo })
        assertEquals("Seguimiento atrasado hace 3 día(s)", estado.alertas.first().detalle)
        assertEquals(USUARIO_DEMO, estado.usuario)
        assertFalse(estado.cargando)
    }

    @Test
    fun muestraElEstadoDeSincronizacion() = runTest(regla.dispatcher) {
        val vm = InicioViewModel(app, repo, auth)
        backgroundScope.launch { vm.estado.collect {} }

        sync.value = EstadoSync.SinConexion("Sin red")

        assertEquals(EstadoSync.SinConexion("Sin red"), vm.estado.value.sync)
    }

    @Test
    fun alIniciar_refrescaYSincroniza() {
        InicioViewModel(app, repo, auth)

        coVerify { repo.refrescar() }
        verify(exactly = 1) { repo.sincronizarEnSegundoPlano() }
    }

    @Test
    fun botonesSincronizarYCerrarSesion_usanLosRepositorios() {
        val vm = InicioViewModel(app, repo, auth)

        vm.sincronizar()
        vm.cerrarSesion()

        verify(exactly = 2) { repo.sincronizarEnSegundoPlano() }
        verify(exactly = 1) { auth.cerrarSesion() }
    }
}
