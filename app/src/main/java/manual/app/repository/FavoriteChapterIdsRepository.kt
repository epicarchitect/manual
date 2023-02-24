package manual.app.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import manual.app.database.FavoriteChapterIdEntity
import manual.app.database.FavoriteChapterIdsDao
import manual.core.coroutines.flow.mapItems

class FavoriteChapterIdsRepository(private val favoriteChapterIdsDao: FavoriteChapterIdsDao) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun setFavoriteChapterId(chapterId: Int, isFavorite: Boolean) = coroutineScope.launch {
        if (isFavorite) {
            favoriteChapterIdsDao.insert(FavoriteChapterIdEntity(chapterId))
        } else {
            favoriteChapterIdsDao.delete(chapterId)
        }
    }

    fun isFavoriteChapterFlow(chapterId: Int) =
        favoriteChapterIdsDao.entityFlow(chapterId).map { it != null }

    fun favoriteChapterIdsFlow() = favoriteChapterIdsDao.entitiesFlow().mapItems { it.chapterId }

}