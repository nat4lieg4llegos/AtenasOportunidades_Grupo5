package cl.duoc.gestionoportunidades.viewmodels

import android.app.Application
import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.repository.AuthRepository
import cl.duoc.gestionoportunidades.repository.OportunidadRepository
import cl.duoc.gestionoportunidades.util.ReglaDispatcherPrincipal
import cl.duoc.gestionoportunidades.util.USUARIO_DEMO
import cl.duoc.gestionoportunidades.util.oportunidadDePrueba
import cl.duoc.gestionoportunidades.validacion.CampoSeguimiento
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SeguimientoFormViewModelTest {

    @get:Rule
    val regla = ReglaDispatcherPrincipal()

    private val app: Application = mockk(relaxed = true)
    private val repo: OportunidadRepository = mockk(relaxed = true)
    private val auth: AuthRepository = mockk()
    private val oportunidad = oportunidadDePrueba(id = 1)
    private val hoy = LocalDate.now()

    @Before
    fun preparar() {
        coEvery { repo.obtener(1) } returns oportunidad
        every { auth.usuarioActual() } returns USUARIO_DEMO
    }

    private fun crear() = SeguimientoFormViewModel(app, repo, auth)

    @Test
    fun cargar_obtieneLaOportunidad() {
        val vm = crear()

        vm.cargar(1)

        assertEquals(oportunidad, vm.estado.value.oportunidad)
        assertEquals(Fechas.aUi(hoy), vm.estado.value.form.fecha)
    }

    @Test
    fun sinOportunidadCargada_noGuarda() {
        val vm = crear()

        vm.guardar()

        coVerify(exactly = 0) { repo.agregarSeguimiento(any()) }
    }

    @Test
    fun guardarIncompleto_marcaErroresYLuegoValidaEnVivo() {
        val vm = crear()
        vm.cargar(1)

        vm.guardar()
        assertTrue(CampoSeguimiento.TIPO_ACCION in vm.estado.value.errores)
        assertTrue(CampoSeguimiento.RESULTADO in vm.estado.value.errores)
        assertEquals(1, vm.estado.value.intentosFallidos)

        vm.onCambio(CampoSeguimiento.TIPO_ACCION, "Llamada")
        assertFalse(CampoSeguimiento.TIPO_ACCION in vm.estado.value.errores)
        assertTrue(CampoSeguimiento.RESULTADO in vm.estado.value.errores)
        coVerify(exactly = 0) { repo.agregarSeguimiento(any()) }
    }

    @Test
    fun guardarSinProximaAccion_registraElSeguimiento() {
        val vm = crear()
        vm.cargar(1)
        vm.onCambio(CampoSeguimiento.TIPO_ACCION, "Llamada")
        vm.onCambio(CampoSeguimiento.RESULTADO, "Se conversó con el área de bienestar")

        vm.guardar()

        assertTrue(vm.estado.value.guardado)
        assertEquals("Seguimiento registrado", vm.estado.value.mensajeGuardado)
        coVerify {
            repo.agregarSeguimiento(match {
                it.oportunidadId == 1L && it.tipoAccion == "Llamada" && it.fecha == Fechas.aIso(hoy) &&
                    it.responsable == "Ejecutiva Demo" && it.fechaProximo == null
            })
        }
    }

    @Test
    fun guardarConProximaAccionSinRecordatorio_guardaLaFechaProxima() {
        val vm = crear()
        vm.cargar(1)
        vm.onCambio(CampoSeguimiento.TIPO_ACCION, "Envío de propuesta")
        vm.onCambio(CampoSeguimiento.RESULTADO, "Propuesta enviada por correo")
        vm.onCambio(CampoSeguimiento.PROXIMA_ACCION, "Llamar para confirmar")
        vm.onCambio(CampoSeguimiento.FECHA_PROXIMO, Fechas.aUi(hoy.plusDays(3)))
        vm.onRecordatorio(false)

        vm.guardar()

        assertFalse(vm.estado.value.form.crearRecordatorio)
        assertEquals("Seguimiento registrado", vm.estado.value.mensajeGuardado)
        coVerify { repo.agregarSeguimiento(match { it.fechaProximo == Fechas.aIso(hoy.plusDays(3)) }) }
    }
}
