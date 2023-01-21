package manual.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
class NoteEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val content: String
)