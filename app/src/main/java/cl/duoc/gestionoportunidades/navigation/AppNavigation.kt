package cl.duoc.gestionoportunidades.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.gestionoportunidades.ui.screens.DetalleScreen
import cl.duoc.gestionoportunidades.ui.screens.FeriadosScreen
import cl.duoc.gestionoportunidades.ui.screens.InicioScreen
import cl.duoc.gestionoportunidades.ui.screens.ListaScreen
import cl.duoc.gestionoportunidades.ui.screens.LoginScreen
import cl.duoc.gestionoportunidades.ui.screens.OportunidadFormScreen
import cl.duoc.gestionoportunidades.ui.screens.SeguimientoFormScreen
import cl.duoc.gestionoportunidades.viewmodels.MainViewModel

private const val DURACION = 320

@Composable
fun AppNavigation(
    haySesion: Boolean,
    oportunidadInicial: Long?,
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { evento -> ejecutar(navController, evento) }
    }

    LaunchedEffect(oportunidadInicial) {
        if (haySesion && oportunidadInicial != null && oportunidadInicial > 0) {
            viewModel.navigateTo(Screen.Detalle(oportunidadInicial))
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (haySesion) Screen.Inicio.route else Screen.Login.route,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(DURACION)) + fadeIn(tween(DURACION)) },
        exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(DURACION)) { it / 4 } + fadeOut(tween(DURACION)) },
        popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(DURACION)) { it / 4 } + fadeIn(tween(DURACION)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(DURACION)) + fadeOut(tween(DURACION)) }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(alIngresar = {
                viewModel.navigateTo(Screen.Inicio, popUpTo = Screen.Login, inclusive = true)
            })
        }
        composable(Screen.Inicio.route) {
            InicioScreen(
                alVerLista = { viewModel.navigateTo(Screen.Lista) },
                alNueva = { viewModel.navigateTo(Screen.Formulario()) },
                alAbrir = { viewModel.navigateTo(Screen.Detalle(it)) },
                alVerFeriados = { viewModel.navigateTo(Screen.Feriados) },
                alCerrarSesion = { viewModel.navigateTo(Screen.Login, popUpTo = Screen.Inicio, inclusive = true) }
            )
        }
        composable(Screen.Feriados.route) {
            FeriadosScreen(alVolver = { viewModel.navigateUp() })
        }
        composable(Screen.Lista.route) {
            ListaScreen(
                alVolver = { viewModel.navigateUp() },
                alNueva = { viewModel.navigateTo(Screen.Formulario()) },
                alAbrir = { viewModel.navigateTo(Screen.Detalle(it)) }
            )
        }
        composable(Screen.RUTA_FORMULARIO, arguments = listOf(navArgument(Screen.ARG_ID) { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong(Screen.ARG_ID) ?: 0L
            OportunidadFormScreen(
                id = id,
                alVolver = { viewModel.navigateUp() },
                alGuardar = { idGuardado ->
                    if (id == 0L) {
                        viewModel.navigateTo(
                            Screen.Detalle(idGuardado),
                            popUpTo = Screen.Formulario(),
                            inclusive = true,
                            mensaje = "Oportunidad registrada correctamente"
                        )
                    } else {
                        viewModel.navigateBack(mensaje = "Cambios guardados")
                    }
                }
            )
        }
        composable(Screen.RUTA_DETALLE, arguments = listOf(navArgument(Screen.ARG_ID) { type = NavType.LongType })) { entry ->
            val mensaje by entry.savedStateHandle.getStateFlow<String?>(Screen.CLAVE_MENSAJE, null).collectAsStateWithLifecycle()
            DetalleScreen(
                id = entry.arguments?.getLong(Screen.ARG_ID) ?: 0L,
                mensajeExterno = mensaje,
                alConsumirMensaje = { entry.savedStateHandle[Screen.CLAVE_MENSAJE] = null },
                alVolver = { viewModel.navigateUp() },
                alEditar = { viewModel.navigateTo(Screen.Formulario(it)) },
                alNuevoSeguimiento = { viewModel.navigateTo(Screen.NuevoSeguimiento(it)) }
            )
        }
        composable(Screen.RUTA_SEGUIMIENTO, arguments = listOf(navArgument(Screen.ARG_ID) { type = NavType.LongType })) { entry ->
            SeguimientoFormScreen(
                oportunidadId = entry.arguments?.getLong(Screen.ARG_ID) ?: 0L,
                alVolver = { viewModel.navigateUp() },
                alGuardar = { mensaje -> viewModel.navigateBack(mensaje = mensaje) }
            )
        }
    }
}

private fun ejecutar(navController: NavHostController, evento: NavigationEvent) {
    when (evento) {
        is NavigationEvent.NavigateTo -> {
            navController.navigate(evento.route.buildRoute()) {
                evento.popUpToRoute?.let { destino ->
                    popUpTo(destino.route) { inclusive = evento.inclusive }
                }
                launchSingleTop = evento.singleTop
            }
            evento.mensaje?.let { navController.currentBackStackEntry?.savedStateHandle?.set(Screen.CLAVE_MENSAJE, it) }
        }
        is NavigationEvent.PopBackStack -> {
            evento.mensaje?.let { navController.previousBackStackEntry?.savedStateHandle?.set(Screen.CLAVE_MENSAJE, it) }
            navController.popBackStack()
        }
        NavigationEvent.NavigateUp -> navController.navigateUp()
    }
}
