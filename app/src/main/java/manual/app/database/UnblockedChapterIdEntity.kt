package manual.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unblockedChapterIds")
data class UnblockedChapterIdEntity(
    @PrimaryKey
    val chapterId: Int
)