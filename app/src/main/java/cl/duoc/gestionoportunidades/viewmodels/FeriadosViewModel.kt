package cl.duoc.gestionoportunidades.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.gestionoportunidades.data.model.Feriado
import cl.duoc.gestionoportunidades.di.AppContainer
import cl.duoc.gestionoportunidades.repository.FeriadoRepository
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeriadosUiState(
    val anio: Int = LocalDate.now().year,
    val feriados: List<Feriado> = emptyList(),
    val proximo: Feriado? = null,
    val cargando: Boolean = false,
    val error: String? = null
)

class FeriadosViewModel(
    private val repo: FeriadoRepository,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _estado = MutableStateFlow(FeriadosUiState(anio = hoy().year))
    val estado: StateFlow<FeriadosUiState> = _estado.asStateFlow()

    init {
        cargar(_estado.value.anio)
    }

    fun cargar(anio: Int) {
        _estado.update { it.copy(anio = anio, cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val lista = repo.feriados(anio)
                _estado.update {
                    it.copy(
                        feriados = lista,
                        proximo = FeriadoRepository.proximo(lista, hoy()),
                        cargando = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        feriados = emptyList(),
                        proximo = null,
                        cargando = false,
                        error = MENSAJE_ERROR
                    )
                }
            }
        }
    }

    fun anioAnterior() = cargar(_estado.value.anio - 1)

    fun anioSiguiente() = cargar(_estado.value.anio + 1)

    fun reintentar() = cargar(_estado.value.anio)

    companion object {
        const val MENSAJE_ERROR = "No se pudieron cargar los feriados. Revisa tu conexión a internet."

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { FeriadosViewModel(AppContainer.feriados()) }
        }
    }
}
