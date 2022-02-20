package manual.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteChapterIdEntity::class,
        UnblockedChapterIdEntity::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class MainDatabase : RoomDatabase() {

    abstract val favoriteChapterIdsDao: FavoriteChapterIdsDao
    abstract val unblockedChapterIdsDao: UnblockedChapterIdsDao

    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, MainDatabase::class.java, "main")
            .addMigrations(Migration1To2, Migration2To3)
            .build()
    }

    object Migration1To2 : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP TABLE unblockedContentIds")
        }
    }

    object Migration2To3 : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `unblockedChapterIds` (`chapterId` INTEGER NOT NULL, PRIMARY KEY(`chapterId`))")
        }
    }
}