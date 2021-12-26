package manual.app.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import manual.core.resources.mapEachFilePath
import manual.app.data.ChapterGroup
import manual.core.resources.mapEachFile
import manual.core.resources.read

class ChapterGroupsRepository(
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val stateFlow = MutableStateFlow<List<ChapterGroup>?>(null)

    init {
        coroutineScope.launch {
            stateFlow.value = assetManager.mapEachFile("chapter-groups") {
                gson.fromJson(it, ChapterGroup::class.java)
            }
        }
    }

    fun chapterGroupsFlow() = stateFlow.filterNotNull()

}