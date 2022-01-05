package manual.app.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import manual.app.data.Content
import manual.core.resources.mapEachFile

class ContentsRepository(
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val stateFlow = MutableStateFlow<List<Content>?>(null)

    init {
        coroutineScope.launch {
            stateFlow.value = assetManager.mapEachFile("contents") {
                gson.fromJson(it, Content::class.java)
            }
        }
    }

    fun contentsFlow() = stateFlow.filterNotNull()

}