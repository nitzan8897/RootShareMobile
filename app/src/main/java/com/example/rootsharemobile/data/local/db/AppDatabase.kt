package com.example.rootsharemobile.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.rootsharemobile.data.local.db.dao.PlantDao
import com.example.rootsharemobile.data.local.db.dao.PostDao
import com.example.rootsharemobile.data.local.db.dao.UserDao
import com.example.rootsharemobile.data.local.db.entity.PlantEntity
import com.example.rootsharemobile.data.local.db.entity.PostEntity
import com.example.rootsharemobile.data.local.db.entity.UserEntity

/**
 * The Room database for RootShare.
 *
 * This is the Single Source of Truth for all UI data. The app never
 * reads directly from the network for display purposes — data is
 * fetched from the API, written here, and then observed by the UI.
 *
 * NOTE: Firebase/remote offline persistence is intentionally disabled;
 * Room fulfils that role exclusively.
 */
@Database(
    entities = [UserEntity::class, PlantEntity::class, PostEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun plantDao(): PlantDao
    abstract fun postDao(): PostDao

    companion object {
        private const val DATABASE_NAME = "rootshare_db"

        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Returns the singleton [AppDatabase] instance, creating it if necessary.
         * Thread-safe via double-checked locking.
         */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
