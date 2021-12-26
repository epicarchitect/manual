package manual.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unblockedContentIds")
class UnblockedContentIdEntity(
    @PrimaryKey
    val contentId: Int
)