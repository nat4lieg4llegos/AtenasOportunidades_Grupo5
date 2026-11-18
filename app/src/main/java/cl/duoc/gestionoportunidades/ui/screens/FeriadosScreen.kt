package cl.duoc.gestionoportunidades.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.gestionoportunidades.data.model.Feriado
import cl.duoc.gestionoportunidades.viewmodels.FeriadosViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ESPANOL: Locale = Locale.forLanguageTag("es-CL")
private val FORMATO_LARGO: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ESPANOL)
private val FORMATO_DIA: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", ESPANOL)
private val FORMATO_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", ESPANOL)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeriadosScreen(
    alVolver: () -> Unit,
    vm: FeriadosViewModel = viewModel(factory = FeriadosViewModel.Factory)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val error = estado.error
    val proximo = estado.proximo
    val hoy = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feriados en Chile") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = vm::anioAnterior, enabled = !estado.cargando) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Año anterior")
                }
                Text(estado.anio.toString(), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = vm::anioSiguiente, enabled = !estado.cargando) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Año siguiente")
                }
            }
            Text(
                "Fechas a considerar al agendar activaciones. Fuente: API pública Nager.Date",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                error != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Text(error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = vm::reintentar) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reintentar")
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (proximo != null) {
                        item { TarjetaProximoFeriado(proximo) }
                    }
                    items(estado.feriados, key = { "${it.fecha}-${it.nombre}" }) { feriado ->
                        ItemFeriado(feriado, pasado = feriado.fecha.isBefore(hoy))
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaProximoFeriado(feriado: Feriado) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 12.dp)) {
                Text("Próximo feriado", style = MaterialTheme.typography.labelLarge)
                Text(feriado.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    feriado.fecha.format(FORMATO_LARGO).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ItemFeriado(feriado: Feriado, pasado: Boolean) {
    ElevatedCard(Modifier.fillMaxWidth().alpha(if (pasado) 0.55f else 1f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(feriado.fecha.dayOfMonth.toString(), style = MaterialTheme.typography.headlineSmall)
                Text(feriado.fecha.format(FORMATO_MES), style = MaterialTheme.typography.labelMedium)
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(feriado.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    feriado.fecha.format(FORMATO_DIA).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!feriado.nacional) {
                Text("Regional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
