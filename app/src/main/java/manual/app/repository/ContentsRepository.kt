package manual.app.repository

import android.content.res.AssetManager
import android.net.Uri
import androidx.core.text.HtmlCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import manual.app.data.Content
import manual.app.database.UnblockedContentIdEntity
import manual.app.database.UnblockedContentIdsDao
import manual.app.premium.PremiumManager
import manual.core.coroutines.flow.mapItems
import manual.core.resources.mapEachFile
import manual.core.resources.mapEachFilePath
import manual.core.resources.read

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