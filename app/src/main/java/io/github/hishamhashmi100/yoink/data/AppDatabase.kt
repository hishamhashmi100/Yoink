package io.github.hishamhashmi100.yoink.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Download::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "yoink.db").build()
    }
}
