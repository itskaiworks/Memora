package com.example.memora.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoraDao {

    @Query("SELECT * FROM rooms WHERE isArchived = 0 ORDER BY name ASC")
    fun rooms(): Flow<List<RoomEntity>>

    @Query(
        """
        SELECT rooms.*, COUNT(items.id) AS itemCount
        FROM rooms
        LEFT JOIN items ON items.roomId = rooms.id AND items.isArchived = 0
        WHERE rooms.isArchived = 0
        GROUP BY rooms.id
        ORDER BY rooms.name ASC
        """
    )
    fun roomsWithCount(): Flow<List<RoomWithCount>>

    @Query("SELECT * FROM storages WHERE roomId = :roomId AND isArchived = 0 ORDER BY name ASC")
    fun storagesInRoom(roomId: Long): Flow<List<StorageEntity>>

    @Query(
        """
        SELECT items.*, rooms.name AS roomName, storages.name AS storageName
        FROM items
        LEFT JOIN rooms ON rooms.id = items.roomId
        LEFT JOIN storages ON storages.id = items.storageId
        WHERE items.roomId = :roomId AND items.isArchived = 0
        ORDER BY items.name ASC
        """
    )
    fun itemsInRoom(roomId: Long): Flow<List<ItemWithLocation>>

    @Query(
        """
        SELECT items.*, rooms.name AS roomName, storages.name AS storageName
        FROM items
        LEFT JOIN rooms ON rooms.id = items.roomId
        LEFT JOIN storages ON storages.id = items.storageId
        WHERE items.isArchived = 0
        ORDER BY items.addedAt DESC
        """
    )
    fun items(): Flow<List<ItemWithLocation>>

    @Query(
        """
        SELECT items.*, rooms.name AS roomName, storages.name AS storageName
        FROM items
        LEFT JOIN rooms ON rooms.id = items.roomId
        LEFT JOIN storages ON storages.id = items.storageId
        WHERE items.isFavorite = 1 AND items.isArchived = 0
        ORDER BY items.addedAt DESC
        """
    )
    fun favorites(): Flow<List<ItemWithLocation>>

    @Query(
        """
        SELECT items.*, rooms.name AS roomName, storages.name AS storageName
        FROM items
        LEFT JOIN rooms ON rooms.id = items.roomId
        LEFT JOIN storages ON storages.id = items.storageId
        WHERE items.isArchived = 0
        ORDER BY items.lastUpdatedAt DESC
        LIMIT :limit
        """
    )
    fun recent(limit: Int): Flow<List<ItemWithLocation>>

    /**
     * Unfiltered by design: this is a plain lookup by id used to read an item's current details
     * (for History lines, or a "last saved" summary) rather than to decide what belongs in an
     * active list, so it still finds a row the instant after it's been archived.
     */
    @Query(
        """
        SELECT items.*, rooms.name AS roomName, storages.name AS storageName
        FROM items
        LEFT JOIN rooms ON rooms.id = items.roomId
        LEFT JOIN storages ON storages.id = items.storageId
        WHERE items.id = :itemId
        """
    )
    suspend fun itemById(itemId: Long): ItemWithLocation?

    @Query("SELECT * FROM rooms WHERE id = :roomId")
    suspend fun roomById(roomId: Long): RoomEntity?

    @Query("SELECT * FROM storages WHERE id = :storageId")
    suspend fun storageById(storageId: Long): StorageEntity?

    @Insert
    suspend fun insertRoom(room: RoomEntity): Long

    @Insert
    suspend fun insertStorage(storage: StorageEntity): Long

    @Insert
    suspend fun insertItem(item: ItemEntity): Long

    @Query("UPDATE items SET isFavorite = :isFavorite, lastUpdatedAt = :updatedAt WHERE id = :itemId")
    suspend fun setFavorite(itemId: Long, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE items SET isFound = 1, isArchived = 1, archivedAt = :updatedAt, lastUpdatedAt = :updatedAt WHERE id = :itemId")
    suspend fun markFound(itemId: Long, updatedAt: Long)

    /** Permanent delete — only ever called from the Archive's trash icon. An everyday "delete"
     *  elsewhere in the app goes through [archiveRoom] instead. */
    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    @Query("UPDATE rooms SET name = :name, photoPath = :photoPath WHERE id = :roomId")
    suspend fun updateRoom(roomId: Long, name: String, photoPath: String?)

    @Query("UPDATE rooms SET isArchived = 1, archivedAt = :archivedAt WHERE id = :roomId")
    suspend fun archiveRoom(roomId: Long, archivedAt: Long)

    @Query("UPDATE rooms SET isArchived = 0, archivedAt = NULL WHERE id = :roomId")
    suspend fun restoreRoom(roomId: Long)

    @Query("SELECT * FROM rooms WHERE isArchived = 1 ORDER BY archivedAt DESC")
    fun archivedRooms(): Flow<List<RoomEntity>>

    @Query(
        """
        SELECT rooms.*, COUNT(items.id) AS itemCount
        FROM rooms
        LEFT JOIN items ON items.roomId = rooms.id
        WHERE rooms.isArchived = 1
        GROUP BY rooms.id
        ORDER BY rooms.archivedAt DESC
        """
    )
    fun archivedRoomsWithCount(): Flow<List<RoomWithCount>>

    @Query("SELECT * FROM storages WHERE isArchived = 0 ORDER BY name ASC")
    fun storages(): Flow<List<StorageEntity>>

    @Query(
        """
        SELECT storages.*, rooms.name AS roomName, COUNT(items.id) AS itemCount
        FROM storages
        LEFT JOIN rooms ON rooms.id = storages.roomId
        LEFT JOIN items ON items.storageId = storages.id AND items.isArchived = 0
        WHERE storages.isArchived = 0
        GROUP BY storages.id
        ORDER BY storages.name ASC
        """
    )
    fun storagesWithRoom(): Flow<List<StorageWithRoom>>

    @Query(
        """
        SELECT storages.*, rooms.name AS roomName, COUNT(items.id) AS itemCount
        FROM storages
        LEFT JOIN rooms ON rooms.id = storages.roomId
        LEFT JOIN items ON items.storageId = storages.id
        WHERE storages.isArchived = 1
        GROUP BY storages.id
        ORDER BY storages.archivedAt DESC
        """
    )
    fun archivedStoragesWithRoom(): Flow<List<StorageWithRoom>>

    /**
     * Matches the query against the item name, its note, and the room/storage it is filed in, so
     * "kitchen" finds everything in the kitchen and not just an item called kitchen.
     */
    @Query(
        """
        SELECT items.*, rooms.name AS roomName, storages.name AS storageName
        FROM items
        LEFT JOIN rooms ON rooms.id = items.roomId
        LEFT JOIN storages ON storages.id = items.storageId
        WHERE items.isArchived = 0
          AND (
            items.name LIKE '%' || :query || '%'
            OR items.description LIKE '%' || :query || '%'
            OR rooms.name LIKE '%' || :query || '%'
            OR storages.name LIKE '%' || :query || '%'
          )
        ORDER BY items.lastUpdatedAt DESC
        """
    )
    fun search(query: String): Flow<List<ItemWithLocation>>

    /**
     * A flow rather than a one-shot read: the detail screen has to redraw itself after EDIT,
     * favouriting or "I FOUND IT!" write to the row it is showing. It returns a list rather than a
     * nullable row because a deleted (or, now, archived) item should emit "nothing", which Room
     * expresses as an empty result set — the screens that collect this already handle that.
     */
    @Query(
        """
        SELECT items.*, rooms.name AS roomName, storages.name AS storageName
        FROM items
        LEFT JOIN rooms ON rooms.id = items.roomId
        LEFT JOIN storages ON storages.id = items.storageId
        WHERE items.id = :itemId AND items.isArchived = 0
        """
    )
    fun itemRows(itemId: Long): Flow<List<ItemWithLocation>>

    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    fun events(): Flow<List<EventEntity>>

    @Insert
    suspend fun insertEvent(event: EventEntity): Long

    @Query(
        """
        UPDATE items
        SET name = :name, description = :description, photoPath = :photoPath, roomId = :roomId,
            storageId = :storageId, isArchived = 0, archivedAt = NULL, lastUpdatedAt = :updatedAt
        WHERE id = :itemId
        """
    )
    suspend fun updateItem(
        itemId: Long,
        name: String,
        description: String?,
        photoPath: String?,
        roomId: Long?,
        storageId: Long?,
        updatedAt: Long
    )

    @Query("UPDATE items SET photoPath = :photoPath, lastUpdatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemPhoto(itemId: Long, photoPath: String, updatedAt: Long)

    /** Permanent delete — only ever called from the Archive's trash icon. An everyday "delete"
     *  elsewhere in the app goes through [archiveItem] instead. */
    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("UPDATE items SET isArchived = 1, archivedAt = :archivedAt WHERE id = :itemId")
    suspend fun archiveItem(itemId: Long, archivedAt: Long)

    @Query("UPDATE items SET isArchived = 1, archivedAt = :archivedAt WHERE roomId = :roomId AND isArchived = 0")
    suspend fun archiveItemsInRoom(roomId: Long, archivedAt: Long)

    @Query("UPDATE items SET isArchived = 1, archivedAt = :archivedAt WHERE storageId = :storageId AND isArchived = 0")
    suspend fun archiveItemsInStorage(storageId: Long, archivedAt: Long)

    @Query("UPDATE items SET storageId = NULL WHERE storageId = :storageId AND isArchived = 0")
    suspend fun unassignItemsInStorage(storageId: Long)

    @Query("UPDATE items SET isArchived = 0, archivedAt = NULL WHERE id = :itemId")
    suspend fun restoreItem(itemId: Long)

    @Query("UPDATE items SET storageId = NULL, isArchived = 0, archivedAt = NULL, lastUpdatedAt = :updatedAt WHERE id = :itemId")
    suspend fun restoreItemUnassigned(itemId: Long, updatedAt: Long)

    /** Only restores items archived at the exact same moment as the room, so an item that was
     *  independently archived earlier (or later) doesn't come back along for the ride. */
    @Query(
        "UPDATE items SET isArchived = 0, archivedAt = NULL " +
                "WHERE roomId = :roomId AND isArchived = 1 AND archivedAt = :archivedAt"
    )
    suspend fun restoreItemsArchivedWithRoom(roomId: Long, archivedAt: Long)

    @Query(
        "UPDATE items SET storageId = NULL, isArchived = 0, archivedAt = NULL " +
                "WHERE roomId = :roomId AND isArchived = 1 AND archivedAt = :archivedAt"
    )
    suspend fun restoreItemsArchivedWithRoomUnassignedStorage(roomId: Long, archivedAt: Long)

    @Query(
        "UPDATE items SET isArchived = 0, archivedAt = NULL " +
                "WHERE storageId = :storageId AND isArchived = 1 AND archivedAt = :archivedAt"
    )
    suspend fun restoreItemsArchivedWithStorage(storageId: Long, archivedAt: Long)

    @Query(
        """
        SELECT items.*, rooms.name AS roomName, storages.name AS storageName
        FROM items
        LEFT JOIN rooms ON rooms.id = items.roomId
        LEFT JOIN storages ON storages.id = items.storageId
        WHERE items.isArchived = 1
        ORDER BY items.archivedAt DESC
        """
    )
    fun archivedItems(): Flow<List<ItemWithLocation>>

    /** Permanent delete — only ever called from the Archive's trash icon. An everyday "delete"
     *  elsewhere in the app goes through [archiveStorage] instead. */
    @Delete
    suspend fun deleteStorage(storage: StorageEntity)

    @Query("UPDATE storages SET name = :name, photoPath = :photoPath, roomId = :roomId WHERE id = :storageId")
    suspend fun updateStorage(storageId: Long, name: String, photoPath: String?, roomId: Long)

    @Query("UPDATE storages SET isArchived = 1, archivedAt = :archivedAt WHERE id = :storageId")
    suspend fun archiveStorage(storageId: Long, archivedAt: Long)

    /** Cascade half of archiving a room: every storage in it that isn't already archived. */
    @Query("UPDATE storages SET isArchived = 1, archivedAt = :archivedAt WHERE roomId = :roomId AND isArchived = 0")
    suspend fun archiveStoragesInRoom(roomId: Long, archivedAt: Long)

    @Query("UPDATE storages SET isArchived = 0, archivedAt = NULL WHERE id = :storageId")
    suspend fun restoreStorage(storageId: Long)

    /** Only restores storages archived at the exact same moment as the room — see
     *  [restoreItemsArchivedWithRoom] for why. */
    @Query(
        "UPDATE storages SET isArchived = 0, archivedAt = NULL " +
                "WHERE roomId = :roomId AND isArchived = 1 AND archivedAt = :archivedAt"
    )
    suspend fun restoreStoragesArchivedWithRoom(roomId: Long, archivedAt: Long)

    @Query("SELECT COUNT(*) FROM rooms WHERE isArchived = 0")
    suspend fun roomCount(): Int

    @Query("SELECT * FROM profile WHERE id = 1")
    fun profile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun profileOnce(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    // -------------------------------------------------------------------- automatic archive purge

    @Query("DELETE FROM items WHERE isArchived = 1 AND archivedAt <= :cutoff")
    suspend fun purgeExpiredArchivedItems(cutoff: Long)

    @Query("DELETE FROM storages WHERE isArchived = 1 AND archivedAt <= :cutoff")
    suspend fun purgeExpiredArchivedStorages(cutoff: Long)

    @Query("DELETE FROM rooms WHERE isArchived = 1 AND archivedAt <= :cutoff")
    suspend fun purgeExpiredArchivedRooms(cutoff: Long)
}