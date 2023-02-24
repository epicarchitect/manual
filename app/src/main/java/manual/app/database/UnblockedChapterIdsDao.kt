package manual.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnblockedChapterIdsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UnblockedChapterIdEntity)

    @Query("DELETE FROM unblockedChapterIds WHERE chapterId = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM unblockedChapterIds WHERE chapterId = :id")
    fun entityFlow(id: Int): Flow<UnblockedChapterIdEntity?>

    @Query("SELECT * FROM unblockedChapterIds")
    fun entitiesFlow(): Flow<List<UnblockedChapterIdEntity>>

}