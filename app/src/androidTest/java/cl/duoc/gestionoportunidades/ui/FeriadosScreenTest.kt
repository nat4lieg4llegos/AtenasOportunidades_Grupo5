package cl.duoc.gestionoportunidades.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.duoc.gestionoportunidades.data.remote.FeriadoDto
import cl.duoc.gestionoportunidades.data.remote.FeriadosApiService
import cl.duoc.gestionoportunidades.repository.FeriadoRepository
import cl.duoc.gestionoportunidades.ui.screens.FeriadosScreen
import cl.duoc.gestionoportunidades.ui.theme.GestionOportunidadesTheme
import cl.duoc.gestionoportunidades.viewmodels.FeriadosViewModel
import java.io.IOException
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeriadosScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val hoy = LocalDate.of(2026, 11, 18)

    private val apiConDatos = object : FeriadosApiService {
        override suspend fun feriados(anio: Int, pais: String): List<FeriadoDto> = listOf(
            FeriadoDto(date = "$anio-09-18", localName = "Independencia Nacional"),
            FeriadoDto(date = "$anio-12-25", localName = "Navidad")
        )
    }

    private val apiSinConexion = object : FeriadosApiService {
        override suspend fun feriados(anio: Int, pais: String): List<FeriadoDto> = throw IOException("sin red")
    }

    @Test
    fun muestraElProximoFeriadoYLaListaDelAnio() {
        var volvio = false
        val vm = FeriadosViewModel(FeriadoRepository(apiConDatos)) { hoy }
        compose.setContent {
            GestionOportunidadesTheme {
                FeriadosScreen(alVolver = { volvio = true }, vm = vm)
            }
        }

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Navidad").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("2026").assertIsDisplayed()
        compose.onNodeWithText("Próximo feriado").assertIsDisplayed()
        compose.onAllNodesWithText("Navidad").assertCountEquals(2)
        compose.onNodeWithText("Independencia Nacional").assertIsDisplayed()

        compose.onNodeWithContentDescription("Volver").performClick()
        assertTrue(volvio)
    }

    @Test
    fun sinConexion_muestraElErrorYElBotonReintentar() {
        val vm = FeriadosViewModel(FeriadoRepository(apiSinConexion)) { hoy }
        compose.setContent {
            GestionOportunidadesTheme {
                FeriadosScreen(alVolver = {}, vm = vm)
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(FeriadosViewModel.MENSAJE_ERROR).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(FeriadosViewModel.MENSAJE_ERROR).assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertIsDisplayed()
    }
}
