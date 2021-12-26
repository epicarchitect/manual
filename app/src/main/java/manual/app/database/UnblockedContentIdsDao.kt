package manual.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface UnblockedContentIdsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UnblockedContentIdEntity)

    @Query("SELECT * FROM unblockedContentIds")
    fun entitiesFlow(): Flow<List<UnblockedContentIdEntity>>

}