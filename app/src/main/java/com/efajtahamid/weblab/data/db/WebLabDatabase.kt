package com.efajtahamid.weblab.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WebLabDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile private var instance: WebLabDatabase? = null

        fun getInstance(context: Context): WebLabDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WebLabDatabase::class.java,
                    "weblab.db"
                ).build().also { instance = it }
            }
        }
    }
}
