package manual.core.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

abstract class CoreViewModel<STATE> : ViewModel() {

    private val _state = MutableStateFlow<STATE?>(null)
    val state: StateFlow<STATE?> = _state

    protected fun updateState(action: (STATE?) -> STATE?) {
        _state.value = action(_state.value)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    protected fun Flow<*>.launch() = launchIn(coroutineScope)

    protected fun launch(block: suspend CoroutineScope.() -> Unit) = coroutineScope.launch(block = block)

    override fun onCleared() {
        coroutineScope.cancel()
    }
}