package manual.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteChapterIdsDao {

   @Insert(onConflict = OnConflictStrategy.REPLACE)
   suspend fun insert(entity: FavoriteChapterIdEntity)

   @Query("DELETE FROM favoriteChapterIds WHERE chapterId = :id")
   suspend fun delete(id: Int)

   @Query("SELECT * FROM favoriteChapterIds WHERE chapterId = :id")
   fun entityFlow(id: Int): Flow<FavoriteChapterIdEntity?>
   
   @Query("SELECT * FROM favoriteChapterIds")
   fun entitiesFlow(): Flow<List<FavoriteChapterIdEntity>>

}