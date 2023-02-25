package manual.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM notes WHERE id = :id")
    fun entityFlow(id: Int): Flow<NoteEntity?>

    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun entitiesFlow(): Flow<List<NoteEntity>>
}