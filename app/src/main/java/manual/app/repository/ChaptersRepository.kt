package manual.app.repository

import android.content.res.AssetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import manual.app.data.Chapter
import manual.app.parser.ChapterParser
import manual.core.resources.mapEachInputStream

class ChaptersRepository(
    assetManager: AssetManager,
    private val chapterParser: ChapterParser
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val stateFlow = MutableStateFlow<List<Chapter>?>(null)

    init {
        coroutineScope.launch {
            stateFlow.value = assetManager.mapEachInputStream("chapters") {
                it.use(chapterParser::parse)
            }
        }
    }

    fun chaptersFlow() = stateFlow.filterNotNull()

    fun chapterFlow(id: Int) = chaptersFlow().map {
        it.find { it.id == id }
    }.filterNotNull()

}