package com.example.memora.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class RestoreCheckResult {
    ROOM_DELETED,
    STORAGE_DELETED,
    CAN_RESTORE
}

/**
 * Thin wrapper around [MemoraDao]. Holds no state of its own — every call goes straight to Room.
 *
 * Anything that changes an item also writes an [EventEntity], so the History screen is a record of
 * what happened rather than a separate list that has to be kept in sync by hand.
 */
class MemoraRepository(private val dao: MemoraDao) {

    val rooms: Flow<List<RoomEntity>> = dao.rooms()
    val roomsWithCount: Flow<List<RoomWithCount>> = dao.roomsWithCount()
    val storages: Flow<List<StorageEntity>> = dao.storages()
    val storagesWithRoom: Flow<List<StorageWithRoom>> = dao.storagesWithRoom()
    val items: Flow<List<ItemWithLocation>> = dao.items()
    val favorites: Flow<List<ItemWithLocation>> = dao.favorites()
    val events: Flow<List<EventEntity>> = dao.events()
    val profile: Flow<ProfileEntity?> = dao.profile()

    // -------------------------------------------------------------------- archive

    val archivedRoomsWithCount: Flow<List<RoomWithCount>> = dao.archivedRoomsWithCount()
    val archivedStoragesWithRoom: Flow<List<StorageWithRoom>> = dao.archivedStoragesWithRoom()
    val archivedItems: Flow<List<ItemWithLocation>> = dao.archivedItems()

    /** Archives a room and, alongside it, every storage and item still filed in it that isn't
     *  already archived on its own — all three stamped with the same instant, so [restoreRoom]
     *  can tell exactly what to bring back with it. */
    suspend fun archiveRoom(room: RoomEntity) {
        logEventForRoom(room, ItemAction.ARCHIVED)
        val archivedAt = System.currentTimeMillis()
        dao.archiveStoragesInRoom(room.id, archivedAt)
        dao.archiveItemsInRoom(room.id, archivedAt)
        dao.archiveRoom(room.id, archivedAt)
    }

    /** Restores a room with user choices on whether to restore its associated storages and items. */
    suspend fun restoreRoom(
        roomId: Long,
        restoreStorage: Boolean = true,
        restoreItems: Boolean = true
    ) {
        val room = dao.roomById(roomId)
        room?.let { logEventForRoom(it, ItemAction.RESTORED) }
        val archivedAt = room?.archivedAt ?: run {
            dao.restoreRoom(roomId)
            return
        }

        dao.restoreRoom(roomId)

        if (restoreStorage) {
            dao.restoreStoragesArchivedWithRoom(roomId, archivedAt)
        }

        if (restoreItems) {
            if (restoreStorage) {
                dao.restoreItemsArchivedWithRoom(roomId, archivedAt)
            } else {
                dao.restoreItemsArchivedWithRoomUnassignedStorage(roomId, archivedAt)
            }
        }
    }

    /** Permanent delete, straight out of the Archive. Storages and items in the room that are
     *  still archived go with it (the real @Delete's CASCADE only reaches storages, not items —
     *  see [ItemEntity]'s SET_NULL foreign keys — so items are cleaned up explicitly here). */
    suspend fun permanentlyDeleteRoom(room: RoomEntity) {
        PhotoStore.deletePhoto(room.photoPath)
        logEventForRoom(room, ItemAction.DELETED)
        dao.deleteRoom(room)
    }

    /** Archives a storage and, alongside it, every item still filed in it that isn't already
     *  archived on its own, both stamped with the same instant. */
    suspend fun archiveStorage(storage: StorageEntity, archiveItems: Boolean = true) {
        logEventForStorage(storage, ItemAction.ARCHIVED)
        val archivedAt = System.currentTimeMillis()
        if (archiveItems) {
            dao.archiveItemsInStorage(storage.id, archivedAt)
        } else {
            dao.unassignItemsInStorage(storage.id)
        }
        dao.archiveStorage(storage.id, archivedAt)
    }

    suspend fun restoreStorage(storageId: Long, restoreItems: Boolean = true) {
        val storage = dao.storageById(storageId)
        storage?.let { logEventForStorage(it, ItemAction.RESTORED) }
        val archivedAt = storage?.archivedAt ?: run {
            dao.restoreStorage(storageId)
            return
        }
        dao.restoreStorage(storageId)
        if (restoreItems) {
            dao.restoreItemsArchivedWithStorage(storageId, archivedAt)
        }
    }

    suspend fun permanentlyDeleteStorage(storage: StorageEntity) {
        PhotoStore.deletePhoto(storage.photoPath)
        logEventForStorage(storage, ItemAction.DELETED)
        dao.deleteStorage(storage)
    }

    /** Everyday "delete" on an item, from Edit Item — moves it to the Archive instead of removing
     *  it. The history line is written *before* the archive write, same as the old hard delete
     *  used to, so it can still read the item's location. */
    suspend fun archiveItem(itemId: Long) {
        logEvent(itemId, ItemAction.ARCHIVED)
        dao.archiveItem(itemId, System.currentTimeMillis())
    }

    /** Restores an item to exactly the room/storage it already remembers — archiving an item
     *  never touches its `roomId`/`storageId`, so there's nothing left to look up here. */
    suspend fun restoreItem(itemId: Long) {
        dao.restoreItem(itemId)
        logEvent(itemId, ItemAction.RESTORED)
    }

    suspend fun restoreItemUnassigned(itemId: Long) {
        dao.restoreItemUnassigned(itemId, System.currentTimeMillis())
        logEvent(itemId, ItemAction.RESTORED)
    }

    suspend fun checkItemRestoreStatus(itemId: Long): RestoreCheckResult {
        val row = dao.itemById(itemId) ?: return RestoreCheckResult.ROOM_DELETED
        val roomId = row.item.roomId ?: return RestoreCheckResult.ROOM_DELETED
        val room = dao.roomById(roomId)
        if (room == null || room.isArchived) return RestoreCheckResult.ROOM_DELETED

        val storageId = row.item.storageId
        if (storageId != null) {
            val storage = dao.storageById(storageId)
            if (storage == null || storage.isArchived) return RestoreCheckResult.STORAGE_DELETED
        }

        return RestoreCheckResult.CAN_RESTORE
    }

    suspend fun checkStorageRestoreStatus(storageId: Long): RestoreCheckResult {
        val storage = dao.storageById(storageId) ?: return RestoreCheckResult.ROOM_DELETED
        val room = dao.roomById(storage.roomId)
        if (room == null || room.isArchived) return RestoreCheckResult.ROOM_DELETED

        return RestoreCheckResult.CAN_RESTORE
    }

    /** Permanent delete, straight out of the Archive. The history line is written first, exactly
     *  like the old hard delete this replaces, since the row won't exist to read afterward. */
    suspend fun permanentlyDeleteItem(itemId: Long) {
        logEvent(itemId, ItemAction.DELETED)
        dao.deleteItem(itemId)
    }

    /**
     * Automatically purges archived items, storages, and rooms older than [retentionDays].
     * Order of deletion: Items -> Storages -> Rooms to prevent cascading deletions
     * from removing items/storages before they reach retention cutoff.
     */
    suspend fun purgeExpiredArchive(retentionDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000L)
        dao.purgeExpiredArchivedItems(cutoff)
        dao.purgeExpiredArchivedStorages(cutoff)
        dao.purgeExpiredArchivedRooms(cutoff)
    }

    // -------------------------------------------------------------------- everything else

    fun recent(limit: Int = 10): Flow<List<ItemWithLocation>> = dao.recent(limit)

    fun storagesInRoom(roomId: Long): Flow<List<StorageEntity>> = dao.storagesInRoom(roomId)

    fun itemsInRoom(roomId: Long): Flow<List<ItemWithLocation>> = dao.itemsInRoom(roomId)

    fun search(query: String): Flow<List<ItemWithLocation>> = dao.search(query.trim())

    fun itemFlow(itemId: Long): Flow<ItemWithLocation?> =
        dao.itemRows(itemId).map { it.firstOrNull() }

    suspend fun itemById(itemId: Long): ItemWithLocation? = dao.itemById(itemId)

    suspend fun roomById(roomId: Long): RoomEntity? = dao.roomById(roomId)

    suspend fun storageById(storageId: Long): StorageEntity? = dao.storageById(storageId)

    /** Nothing to backfill — the app starts empty and fills in as the user adds things. */
    suspend fun seedIfEmpty() {
        // Intentionally left empty: no fabricated demo data.
    }

    suspend fun saveRoom(roomName: String, roomPhoto: String?): Long =
        dao.insertRoom(RoomEntity(name = roomName.trim(), photoPath = roomPhoto))

    suspend fun updateRoom(roomId: Long, name: String, photoPath: String?) =
        dao.updateRoom(roomId, name.trim(), photoPath)

    suspend fun saveStorage(roomId: Long, storageName: String, storagePhoto: String?): Long =
        dao.insertStorage(StorageEntity(roomId = roomId, name = storageName.trim(), photoPath = storagePhoto))

    suspend fun updateStorage(storageId: Long, name: String, photoPath: String?, roomId: Long) =
        dao.updateStorage(storageId, name.trim(), photoPath, roomId)

    suspend fun saveItem(
        itemName: String,
        roomId: Long?,
        storageId: Long?,
        photoPath: String?,
        description: String? = null
    ): Long {
        val id = dao.insertItem(
            ItemEntity(
                name = itemName.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                photoPath = photoPath,
                roomId = roomId,
                storageId = storageId
            )
        )
        logEvent(id, ItemAction.ADDED)
        return id
    }

    suspend fun updateItemPhoto(itemId: Long, photoPath: String) {
        dao.updateItemPhoto(itemId, photoPath, System.currentTimeMillis())
        logEvent(itemId, ItemAction.EDITED)
    }

    suspend fun updateItem(
        itemId: Long,
        name: String,
        description: String?,
        photoPath: String?,
        roomId: Long?,
        storageId: Long?
    ) {
        dao.updateItem(
            itemId = itemId,
            name = name.trim(),
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            photoPath = photoPath,
            roomId = roomId,
            storageId = storageId,
            updatedAt = System.currentTimeMillis()
        )
        logEvent(itemId, ItemAction.EDITED)
    }

    suspend fun setFavorite(itemId: Long, isFavorite: Boolean) =
        dao.setFavorite(itemId, isFavorite, System.currentTimeMillis())

    suspend fun markFound(itemId: Long) {
        val row = dao.itemById(itemId) ?: return
        if (row.item.isArchived) return
        logEvent(itemId, ItemAction.RETRIEVED)
        dao.markFound(itemId, System.currentTimeMillis())
    }

    private suspend fun logEvent(itemId: Long, action: ItemAction) {
        val row = dao.itemById(itemId) ?: return
        dao.insertEvent(
            EventEntity(
                itemId = itemId,
                itemName = row.item.name,
                roomName = row.roomName,
                storageName = row.storageName,
                description = row.item.description,
                action = action.name
            )
        )
    }

    private suspend fun logEventForRoom(room: RoomEntity, action: ItemAction) {
        dao.insertEvent(
            EventEntity(
                itemId = null,
                itemName = "ROOM: ${room.name}",
                roomName = room.name,
                storageName = null,
                description = "Room ${action.name.lowercase()}",
                action = action.name
            )
        )
    }

    private suspend fun logEventForStorage(storage: StorageEntity, action: ItemAction) {
        val room = dao.roomById(storage.roomId)
        dao.insertEvent(
            EventEntity(
                itemId = null,
                itemName = "STORAGE: ${storage.name}",
                roomName = room?.name,
                storageName = storage.name,
                description = "Storage ${action.name.lowercase()}",
                action = action.name
            )
        )
    }

    // Read-then-write rather than two independent partial UPDATEs, so a username edit never
    // wipes out a previously chosen photo (and vice versa) — there is only ever one row.
    suspend fun updateProfileUsername(username: String) {
        val current = dao.profileOnce()
        dao.upsertProfile(ProfileEntity(username = username, photoPath = current?.photoPath))
    }

    suspend fun updateProfilePhoto(photoPath: String) {
        val current = dao.profileOnce()
        dao.upsertProfile(ProfileEntity(username = current?.username.orEmpty(), photoPath = photoPath))
    }
}