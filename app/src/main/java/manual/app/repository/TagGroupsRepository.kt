package manual.app.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import manual.core.resources.mapEachFilePath
import manual.app.data.TagGroup
import manual.core.resources.read

class TagGroupsRepository(
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val stateFlow = MutableStateFlow<List<TagGroup>?>(null)

    init {
        coroutineScope.launch {
            stateFlow.value = assetManager.mapEachFilePath("tag-groups") {
                gson.fromJson(read(it), TagGroup::class.java)
            }
        }
    }

    fun tagGroupsFlow() = stateFlow.filterNotNull()

}