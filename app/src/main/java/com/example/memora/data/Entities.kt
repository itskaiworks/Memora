package com.example.memora.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Marker prefix for a photo path that points at a bundled drawable sample instead of a captured
 * file (see [com.example.memora.rememberPhotoModel]). Lives here, not in the UI layer, so the
 * database never has to store a build-specific resource id.
 */
const val SAMPLE_PHOTO_PREFIX = "sample:"

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val photoPath: String?,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null
)

@Entity(
    tableName = "storages",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StorageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: Long,
    val name: String,
    val photoPath: String?,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = StorageEntity::class,
            parentColumns = ["id"],
            childColumns = ["storageId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val photoPath: String?,
    val roomId: Long?,
    val storageId: Long?,
    val isFavorite: Boolean = false,
    val isFound: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val archivedAt: Long? = null
)

/** An item joined with the room/storage names it's filed under, for display. */
data class ItemWithLocation(
    @Embedded val item: ItemEntity,
    val roomName: String?,
    val storageName: String?
)

/** A room joined with how many items are filed in it, for the rooms list. */
data class RoomWithCount(
    @Embedded val room: RoomEntity,
    @ColumnInfo(name = "itemCount") val itemCount: Int
)

/** The kinds of thing that show up on the History screen. "DELETED" is now only ever written for
 *  a *permanent* delete out of the Archive — an everyday delete moves something to the Archive
 *  instead, which is logged as ARCHIVED (and RESTORED if it's brought back). */
enum class ItemAction { ADDED, RETRIEVED, EDITED, ARCHIVED, RESTORED, DELETED }

/**
 * One line of the History screen. Written whenever an item is added, found, edited or deleted, so
 * History reflects what actually happened rather than a hardcoded sample list. The room/storage
 * names are copied in rather than joined, so a deleted item's history still reads correctly.
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long?,
    val itemName: String,
    val roomName: String?,
    val storageName: String?,
    val description: String?,
    val action: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** A storage joined with the room it sits in and how many items are filed in it. */
data class StorageWithRoom(
    @Embedded val storage: StorageEntity,
    val roomName: String?,
    @ColumnInfo(name = "itemCount") val itemCount: Int
)

/**
 * The user's own profile — a single row (id is always 1, enforced by always writing that same
 * id). A real table rather than SharedPreferences so Profile reads it the same reactive way as
 * everything else in the app, and so it survives process death the same way rooms/items do.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val username: String,
    val photoPath: String?
)