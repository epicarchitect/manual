package manual.core.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn

abstract class CoreViewModel<STATE> : ViewModel() {

    private val _state = MutableStateFlow<STATE?>(null)
    val state: StateFlow<STATE?> = _state
    private var updateJob: Job? = null

    protected fun updateState(action: suspend (STATE?) -> STATE?) {
        updateJob?.cancel()
        updateJob = launch {
            _state.value = action(_state.value)
            updateJob = null
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    protected fun Flow<*>.launch() = launchIn(coroutineScope)

    protected fun launch(block: suspend CoroutineScope.() -> Unit) = coroutineScope.launch(block = block)

    override fun onCleared() {
        coroutineScope.cancel()
    }
}