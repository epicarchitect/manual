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
import manual.app.data.ChapterIcon
import manual.core.resources.read

class ChapterIconsRepository(
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val stateFlow = MutableStateFlow<List<ChapterIcon>?>(null)

    init {
        coroutineScope.launch {
            coroutineScope.launch {
                stateFlow.value = gson.fromJson(
                    assetManager.read("chapter-icons/map.json"),
                    JsonArray::class.java
                ).map {
                    val json = it.asJsonObject
                    ChapterIcon(
                        json["chapterId"].asInt,
                        json["source"].asString
                    )
                }
            }
        }
    }

    fun chapterIconFlow(chapterId: Int) = stateFlow.filterNotNull().map {
        it.firstOrNull { it.chapterId == chapterId }
    }

}