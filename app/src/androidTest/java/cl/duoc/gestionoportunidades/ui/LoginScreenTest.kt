package cl.duoc.gestionoportunidades.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.duoc.gestionoportunidades.ui.screens.LoginScreen
import cl.duoc.gestionoportunidades.ui.theme.GestionOportunidadesTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun ingresarConCamposVacios_muestraLosErroresYNoEntra() {
        var ingreso = false
        compose.setContent {
            GestionOportunidadesTheme {
                LoginScreen(alIngresar = { ingreso = true })
            }
        }

        compose.onNodeWithText("Ingresar").performClick()

        compose.onNodeWithText("Ingresa tu usuario").assertIsDisplayed()
        compose.onNodeWithText("Ingresa tu contraseña").assertIsDisplayed()
        assertFalse(ingreso)
    }
}
