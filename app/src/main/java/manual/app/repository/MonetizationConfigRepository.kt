package manual.app.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import manual.app.data.MonetizationConfig
import manual.core.resources.read

class MonetizationConfigRepository(
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val stateFlow = MutableStateFlow<MonetizationConfig?>(null)

    init {
        coroutineScope.launch {
            stateFlow.value = gson.fromJson(
                assetManager.read("monetization/config.json"),
                MonetizationConfig::class.java
            )
        }
    }

    fun monetizationConfigFlow() = stateFlow.filterNotNull()

}