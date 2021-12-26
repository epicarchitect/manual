package manual.app.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteChapterIdEntity::class,
        UnblockedContentIdEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class MainDatabase : RoomDatabase() {
    abstract val favoriteChapterIdsDao: FavoriteChapterIdsDao
    abstract val unblockedContentIdsDao: UnblockedContentIdsDao
}