package com.logan.vera.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.logan.vera.data.database.dao.BookDao
import com.logan.vera.data.database.entities.Book
import com.logan.vera.data.database.entities.BookCollection
import com.logan.vera.data.database.entities.BookCollectionEntry

@Database(
    entities = [
        Book::class,
        BookCollection::class,
        BookCollectionEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vera_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
