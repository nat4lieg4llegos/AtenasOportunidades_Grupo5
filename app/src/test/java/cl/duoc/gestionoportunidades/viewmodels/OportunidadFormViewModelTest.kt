package cl.duoc.gestionoportunidades.viewmodels

import android.app.Application
import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.nativo.UbicacionAproximada
import cl.duoc.gestionoportunidades.nativo.UbicacionHelper
import cl.duoc.gestionoportunidades.repository.AuthRepository
import cl.duoc.gestionoportunidades.repository.OportunidadRepository
import cl.duoc.gestionoportunidades.util.ReglaDispatcherPrincipal
import cl.duoc.gestionoportunidades.util.USUARIO_DEMO
import cl.duoc.gestionoportunidades.util.oportunidadDePrueba
import cl.duoc.gestionoportunidades.validacion.CampoOportunidad
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OportunidadFormViewModelTest {

    @get:Rule
    val regla = ReglaDispatcherPrincipal()

    private val app: Application = mockk(relaxed = true)
    private val repo: OportunidadRepository = mockk(relaxed = true)
    private val auth: AuthRepository = mockk()
    private val ubicacion: UbicacionHelper = mockk()
    private val hoy = LocalDate.now()

    @Before
    fun preparar() {
        every { auth.usuarioActual() } returns USUARIO_DEMO
    }

    private fun crear() = OportunidadFormViewModel(app, repo, auth, ubicacion)

    private fun completarFormulario(vm: OportunidadFormViewModel) {
        vm.cargar(0)
        vm.onCambio(CampoOportunidad.NOMBRE, "Feria de beneficios laborales")
        vm.onCambio(CampoOportunidad.ORGANIZACION, "Empresa Horizonte Ficticia")
        vm.onCambio(CampoOportunidad.TIPO, "Feria empresarial")
        vm.onCambio(CampoOportunidad.FUENTE, "Sitio web")
        vm.onCambio(CampoOportunidad.FECHA_EVENTO, Fechas.aUi(hoy.plusDays(20)))
        vm.onCambio(CampoOportunidad.HORARIO, "09:00 - 17:00")
        vm.onCambio(CampoOportunidad.COMUNA, "Puente Alto")
        vm.onCambio(CampoOportunidad.REGION, "Metropolitana")
        vm.onCambio(CampoOportunidad.PUBLICO_ESPERADO, "250")
        vm.onDatosFicticios(true)
    }

    @Test
    fun nuevaOportunidad_prellenaFechaDeDeteccionYResponsable() {
        val vm = crear()

        vm.cargar(0)

        val estado = vm.estado.value
        assertEquals(Fechas.aUi(hoy), estado.form.fechaDeteccion)
        assertEquals("Ejecutiva Demo", estado.form.responsable)
        assertFalse(estado.esEdicion)
    }

    @Test
    fun editar_cargaLosDatosGuardados() {
        coEvery { repo.obtener(1) } returns oportunidadDePrueba(id = 1, nombre = "Feria de invierno", fotoRuta = "/fotos/a.jpg")
        val vm = crear()

        vm.cargar(1)

        val estado = vm.estado.value
        assertTrue(estado.esEdicion)
        assertEquals("Feria de invierno", estado.form.nombre)
        assertEquals("250", estado.form.publicoEsperado)
        assertEquals("150000", estado.form.costo)
        assertEquals("/fotos/a.jpg", estado.fotoRuta)
    }

    @Test
    fun editarUnaQueNoExiste_avisa() {
        coEvery { repo.obtener(5) } returns null
        val vm = crear()

        vm.cargar(5)

        assertEquals("No se encontró la oportunidad", vm.estado.value.mensaje)
    }

    @Test
    fun guardarIncompleto_marcaErroresYNoGuarda() {
        val vm = crear()
        vm.cargar(0)

        vm.guardar()

        val estado = vm.estado.value
        assertTrue(CampoOportunidad.NOMBRE in estado.errores)
        assertTrue(CampoOportunidad.DATOS_FICTICIOS in estado.errores)
        assertEquals(1, estado.intentosFallidos)
        assertTrue(estado.mensaje!!.startsWith("Revisa"))
        coVerify(exactly = 0) { repo.guardar(any(), any()) }
    }

    @Test
    fun despuesDeUnErrorLaValidacionEsEnVivo() {
        val vm = crear()
        vm.cargar(0)
        vm.guardar()

        vm.onCambio(CampoOportunidad.NOMBRE, "Feria de verano")

        assertFalse(CampoOportunidad.NOMBRE in vm.estado.value.errores)
        assertTrue(CampoOportunidad.ORGANIZACION in vm.estado.value.errores)
    }

    @Test
    fun camposNumericos_soloAceptanDigitos() {
        val vm = crear()
        vm.cargar(0)

        vm.onCambio(CampoOportunidad.PUBLICO_ESPERADO, "2a5b0")
        vm.onCambio(CampoOportunidad.COSTO, "1.500")

        assertEquals("250", vm.estado.value.form.publicoEsperado)
        assertEquals("1500", vm.estado.value.form.costo)
        assertTrue(vm.estado.value.hayCambios)
    }

    @Test
    fun guardarFormularioValido_enviaLaOportunidadAlRepositorio() {
        coEvery { repo.guardar(any(), any()) } returns 15L
        val vm = crear()
        completarFormulario(vm)

        vm.guardar()

        assertEquals(15L, vm.estado.value.guardadoId)
        assertFalse(vm.estado.value.hayCambios)
        coVerify {
            repo.guardar(
                match {
                    it.id == 0L && it.nombre == "Feria de beneficios laborales" && it.publicoEsperado == 250 &&
                        it.fechaEvento == Fechas.aIso(hoy.plusDays(20))
                },
                "Ejecutiva Demo"
            )
        }
    }

    @Test
    fun propuestaEnviada_seRegistraEnElFormulario() {
        val vm = crear()

        vm.onPropuestaEnviada(true)

        assertTrue(vm.estado.value.form.propuestaEnviada)
    }

    @Test
    fun ubicacionNoDisponible_pideIngresarLaComuna() {
        coEvery { ubicacion.obtenerUbicacion() } returns null
        val vm = crear()

        vm.usarUbicacion()

        assertFalse(vm.estado.value.buscandoUbicacion)
        assertTrue(vm.estado.value.mensajeUbicacion!!.startsWith("No se pudo obtener la ubicación"))
    }

    @Test
    fun ubicacionDetectada_completaComunaYRegion() {
        coEvery { ubicacion.obtenerUbicacion() } returns
            UbicacionAproximada("Pirque", "Región Metropolitana de Santiago", -33.6, -70.5)
        val vm = crear()

        vm.usarUbicacion()

        val estado = vm.estado.value
        assertEquals("Pirque", estado.form.comuna)
        assertEquals("Metropolitana", estado.form.region)
        assertTrue(estado.mensajeUbicacion!!.contains("Pirque"))
    }

    @Test
    fun ubicacionSinComuna_loInforma() {
        coEvery { ubicacion.obtenerUbicacion() } returns UbicacionAproximada(null, null, -33.6, -70.5)
        val vm = crear()

        vm.usarUbicacion()

        assertTrue(vm.estado.value.mensajeUbicacion!!.contains("no se identificó la comuna"))
    }

    @Test
    fun permisoDeUbicacionDenegado_explicaQueEsOpcional() {
        val vm = crear()

        vm.permisoUbicacionDenegado()

        assertTrue(vm.estado.value.mensajeUbicacion!!.startsWith("Permiso denegado"))
    }

    @Test
    fun quitarFotoYMensajes() {
        val vm = crear()

        vm.quitarFoto()
        assertNull(vm.estado.value.fotoRuta)
        assertTrue(vm.estado.value.hayCambios)

        vm.mostrarMensaje("Foto agregada")
        assertEquals("Foto agregada", vm.estado.value.mensaje)
        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }
}
