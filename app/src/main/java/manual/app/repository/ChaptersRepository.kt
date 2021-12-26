package manual.app.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import manual.core.resources.mapEachFilePath
import manual.app.data.Chapter
import manual.core.coroutines.flow.mapItems
import manual.app.premium.PremiumManager
import manual.app.database.FavoriteChapterIdEntity
import manual.app.database.FavoriteChapterIdsDao
import manual.core.resources.mapEachFile
import manual.core.resources.read

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