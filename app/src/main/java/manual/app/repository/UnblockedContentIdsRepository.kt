package manual.app.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import manual.app.database.UnblockedContentIdEntity
import manual.app.database.UnblockedContentIdsDao
import manual.core.coroutines.flow.mapItems

class UnblockedContentIdsRepository(private val unblockedContentIdsDao: UnblockedContentIdsDao) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun unblockContentId(contentId: Int) = coroutineScope.launch {
        unblockedContentIdsDao.insert(UnblockedContentIdEntity(contentId))
    }

    fun unblockedContentIdsFlow() = unblockedContentIdsDao.entitiesFlow().mapItems { it.contentId }

}