package com.example.guardiantrack

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PendingUpload::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingUploadDao(): PendingUploadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // If INSTANCE is not null, return it,
            // else create the database instance in a thread-safe way
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guardiantrack_db"
                )
                    // You can add fallbackToDestructiveMigration() here if needed during dev
                    //.fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
