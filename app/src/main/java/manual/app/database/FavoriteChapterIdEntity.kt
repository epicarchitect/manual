package manual.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favoriteChapterIds")
data class FavoriteChapterIdEntity(
    @PrimaryKey
    val chapterId: Int
)