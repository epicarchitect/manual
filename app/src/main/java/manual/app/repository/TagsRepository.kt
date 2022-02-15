package manual.app.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import manual.app.data.Tag
import manual.core.resources.mapEachFile

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