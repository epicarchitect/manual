package manual.app.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import manual.app.data.ChapterTags
import manual.core.resources.read

class ChapterTagsRepository(
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val stateFlow = MutableStateFlow<List<ChapterTags>?>(null)

    init {
        coroutineScope.launch {
            try {
                stateFlow.value = gson.fromJson(
                    assetManager.read("chapter-tags/map.json"),
                    JsonArray::class.java
                ).map {
                    val json = it.asJsonObject
                    ChapterTags(
                        json["chapterId"].asInt,
                        json["tagIds"].asJsonArray.map { it.asInt }
                    )
                }
            } catch (t: Throwable) {
                stateFlow.value = emptyList()
            }
        }
    }

    fun chapterTagsFlow() = stateFlow.filterNotNull()

    fun chapterTagsFlow(chapterId: Int) = stateFlow.filterNotNull().map {
        it.firstOrNull { it.chapterId == chapterId }
    }
}