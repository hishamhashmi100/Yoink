package io.github.hishamhashmi100.yoink.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Download::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "yoink.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        /** Version 2 adds each video's resolution. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN resolution INTEGER")
            }
        }

        /** Version 3 adds the playlist a video was downloaded from. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN playlistId TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN playlistTitle TEXT")
            }
        }
    }
}
