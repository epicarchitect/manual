package manual.app.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import manual.app.database.UnblockedChapterIdEntity
import manual.app.database.UnblockedChapterIdsDao
import manual.core.coroutines.flow.mapItems

class UnblockedChapterIdsRepository(private val unblockedChapterIdsDao: UnblockedChapterIdsDao) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun setUnblockedChapterId(chapterId: Int, isUnblocked: Boolean) = coroutineScope.launch {
        if (isUnblocked) {
            unblockedChapterIdsDao.insert(UnblockedChapterIdEntity(chapterId))
        } else {
            unblockedChapterIdsDao.delete(chapterId)
        }
    }

    fun isUnblockedChapterFlow(chapterId: Int) =
        unblockedChapterIdsDao.entityFlow(chapterId).map { it != null }

    fun unblockedChapterIdsFlow() = unblockedChapterIdsDao.entitiesFlow().mapItems { it.chapterId }

}