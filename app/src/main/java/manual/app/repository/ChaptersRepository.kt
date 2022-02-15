package manual.app.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import manual.app.data.Chapter
import manual.core.resources.mapEachFile

class ChaptersRepository(
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val stateFlow = MutableStateFlow<List<Chapter>?>(null)

    init {
        coroutineScope.launch {
            stateFlow.value = assetManager.mapEachFile("chapters") {
                gson.fromJson(it, Chapter::class.java)
            }
        }
    }

    fun chaptersFlow() = stateFlow.filterNotNull()

    fun chapterFlow(id: Int) = chaptersFlow().map {
        it.find { it.id == id }
    }.filterNotNull()

}