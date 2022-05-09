package manual.app.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import manual.app.data.LaunchConfig
import manual.core.resources.read

class LaunchConfigRepository(
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val stateFlow = MutableStateFlow<LaunchConfig?>(null)

    init {
        coroutineScope.launch {
            stateFlow.value = gson.fromJson(
                assetManager.read("app/launch-config.json"),
                LaunchConfig::class.java
            )
        }
    }

    fun launchConfigFlow() = stateFlow.filterNotNull()

}