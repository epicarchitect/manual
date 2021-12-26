package manual.core.coroutines.flow

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.*

fun <T, R> Flow<List<T>>.mapItems(transform: suspend (T) -> R) = map { list ->
    list.map { transform(it) }
}

fun <T> Flow<T>.onEachChanged(action: suspend (T) -> Unit) = distinctUntilChanged().onEach(action)

fun <T> Flow<T>.launchWith(
    lifecycleOwner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED
) = flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState)
    .launchIn(lifecycleOwner.lifecycleScope)
