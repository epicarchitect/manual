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
import manual.app.data.Tag
import manual.app.premium.PremiumManager
import manual.core.resources.mapEachFile
import manual.core.resources.read

class TagsRepository(
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val stateFlow = MutableStateFlow<List<Tag>?>(null)

    init {
        coroutineScope.launch {
            stateFlow.value = assetManager.mapEachFile("tags") {
                gson.fromJson(it, Tag::class.java)
            }
        }
    }

    fun tagsFlow() = stateFlow.filterNotNull()

}