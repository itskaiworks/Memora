package com.example.memora.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RoomEntity::class, StorageEntity::class, ItemEntity::class, EventEntity::class, ProfileEntity::class],
    // 2: adds the events table behind the History screen.
    // 3: adds the profile table behind the editable username/photo on the Profile screen.
    // 4: adds isArchived/archivedAt to rooms, storages and items behind the Archive screen —
    //    "delete" on a room/storage/item now flips this flag instead of removing the row.
    version = 4,
    exportSchema = false
)
abstract class MemoraDatabase : RoomDatabase() {

    abstract fun memoraDao(): MemoraDao

    companion object {
        @Volatile
        private var instance: MemoraDatabase? = null

        fun get(context: Context): MemoraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemoraDatabase::class.java,
                    "memora.db"
                )
                    // Nothing shipped yet, so there is no user data worth migrating between
                    // schema versions; rebuilding is preferable to crashing on launch.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}