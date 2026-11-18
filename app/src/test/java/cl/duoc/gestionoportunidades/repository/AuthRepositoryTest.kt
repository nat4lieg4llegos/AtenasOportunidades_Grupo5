package cl.duoc.gestionoportunidades.repository

import cl.duoc.gestionoportunidades.data.local.SesionManager
import cl.duoc.gestionoportunidades.util.USUARIO_DEMO
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryTest {

    private val sesion: SesionManager = mockk(relaxed = true)
    private val repo = AuthRepository(sesion)

    @Test
    fun credencialesCorrectas_guardanLaSesion() {
        val usuario = repo.iniciarSesion("ejecutivo", "Atenas2026")

        assertEquals("Ejecutiva Demo", usuario?.nombre)
        verify(exactly = 1) { sesion.guardar(usuario!!) }
    }

    @Test
    fun usuarioConMayusculasYEspacios_igualIngresa() {
        assertEquals("Supervisor Demo", repo.iniciarSesion("  Supervisor ", "Atenas2026")?.nombre)
    }

    @Test
    fun claveIncorrecta_noGuardaSesion() {
        assertNull(repo.iniciarSesion("ejecutivo", "OtraClave99"))
        verify(exactly = 0) { sesion.guardar(any()) }
    }

    @Test
    fun usuarioInexistente_devuelveNull() {
        assertNull(repo.iniciarSesion("desconocido", "Atenas2026"))
    }

    @Test
    fun usuarioActualYCerrarSesion_usanElSesionManager() {
        every { sesion.usuarioActual() } returns USUARIO_DEMO

        assertEquals(USUARIO_DEMO, repo.usuarioActual())
        repo.cerrarSesion()
        verify(exactly = 1) { sesion.cerrar() }
    }
}
