package com.example.memora.viewmodel
import com.example.memora.ui.screens.*
import com.example.memora.data.*
import com.example.memora.worker.ArchivePurgeWorker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.memora.data.EventEntity
import com.example.memora.data.ItemWithLocation
import com.example.memora.data.MemoraDatabase
import com.example.memora.data.MemoraRepository
import com.example.memora.data.ProfileEntity
import com.example.memora.data.RestoreCheckResult
import com.example.memora.data.RoomEntity
import com.example.memora.data.RoomWithCount
import com.example.memora.data.StorageEntity
import com.example.memora.data.StorageWithRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A room the user has filled in but not saved yet, on the way to Add Storage. */
data class RoomDraft(val name: String, val photoPath: String?)

/**
 * The item being built across the three steps of the add-item flow
 * (capture photo -> name it -> file it in a room and storage).
 *
 * It lives on the view model rather than in each screen's `remember`, because the flow spans three
 * navigation destinations and can detour into Add Room / Add Storage and come back.
 */
data class ItemDraft(
    val photoPath: String? = null,
    val name: String = "",
    val roomId: Long? = null,
    val storageId: Long? = null,
    val note: String = ""
)

/** What was just saved, so the confirmation screen can describe it. */
data class SavedSummary(
    val heading: String,
    val categoryLabel: String,
    val roomName: String?,
    val storageName: String?,
    val primaryPhoto: String?,
    val secondaryPhoto: String? = null,
    /** Render the two-photo room-and-storage layout, even before either photo is taken. */
    val paired: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)

/**
 * One view model for the whole app, created once by [MainActivity] and handed to every
 * destination.
 *
 * It must NOT be obtained with `viewModel()` inside a `composable { }` block: there the owner is
 * the NavBackStackEntry, so every destination would get its own instance and the state handed
 * between screens — [pendingRoom], [itemDraft], [lastSaved] — would never arrive.
 */
class MemoraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MemoraRepository(MemoraDatabase.get(application).memoraDao())

    /**
     * Eagerly, not WhileSubscribed: a screen that sits off the back stack longer than a stop
     * timeout would otherwise come back to a stale cached value before the re-query lands. These
     * are small queries over a local database, so keeping them hot costs nothing.
     */
    private fun <T> stateOf(flow: Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    val rooms: StateFlow<List<RoomEntity>> = stateOf(repository.rooms, emptyList())
    val roomsWithCount: StateFlow<List<RoomWithCount>> = stateOf(repository.roomsWithCount, emptyList())
    val storagesWithRoom: StateFlow<List<StorageWithRoom>> = stateOf(repository.storagesWithRoom, emptyList())
    val items: StateFlow<List<ItemWithLocation>> = stateOf(repository.items, emptyList())
    val favorites: StateFlow<List<ItemWithLocation>> = stateOf(repository.favorites, emptyList())
    val recentlyAdded: StateFlow<List<ItemWithLocation>> = stateOf(repository.recent(), emptyList())
    val events: StateFlow<List<EventEntity>> = stateOf(repository.events, emptyList())
    val profile: StateFlow<ProfileEntity?> = stateOf(repository.profile, null)

    // ---------------------------------------------------------------- archive

    val archivedRoomsWithCount: StateFlow<List<RoomWithCount>> =
        stateOf(repository.archivedRoomsWithCount, emptyList())
    val archivedStoragesWithRoom: StateFlow<List<StorageWithRoom>> =
        stateOf(repository.archivedStoragesWithRoom, emptyList())
    val archivedItems: StateFlow<List<ItemWithLocation>> =
        stateOf(repository.archivedItems, emptyList())

    private val _lastSaved = MutableStateFlow<SavedSummary?>(null)
    val lastSaved: StateFlow<SavedSummary?> = _lastSaved.asStateFlow()

    private val _pendingRoom = MutableStateFlow<RoomDraft?>(null)
    val pendingRoom: StateFlow<RoomDraft?> = _pendingRoom.asStateFlow()

    /**
     * Set when a room is tapped on the Home dashboard, so All Items can open with that room
     * already applied as its filter instead of landing on "ALL" and making the user pick it
     * again. Consumed (cleared) as soon as All Items reads it, so a later plain tab switch to
     * All Items doesn't re-apply a stale room.
     */
    private val _pendingRoomFilter = MutableStateFlow<String?>(null)
    val pendingRoomFilter: StateFlow<String?> = _pendingRoomFilter.asStateFlow()

    fun requestRoomFilter(roomName: String) {
        _pendingRoomFilter.value = roomName
    }

    fun consumeRoomFilter() {
        _pendingRoomFilter.value = null
    }

    private val _itemDraft = MutableStateFlow(ItemDraft())
    val itemDraft: StateFlow<ItemDraft> = _itemDraft.asStateFlow()

    // FIX: recent searches are saved on the device so they survive closing the app.
    private val searchPrefs = application.getSharedPreferences("memora_search", Context.MODE_PRIVATE)

    private val _recentSearches = MutableStateFlow(
        searchPrefs.getString("recent_searches", "").orEmpty()
            .split("\n")
            .filter { it.isNotBlank() }
            .take(MAX_RECENT_SEARCHES)
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    /**
     * Where the current add flow was started from, so DONE on the confirmation screen returns
     * there instead of always dumping the user on Home.
     */
    var addFlowOrigin: String? = null
        private set

    fun setAddFlowOrigin(route: String) {
        addFlowOrigin = route
    }

    /** Remembers what was actually searched for, replacing the fabricated "recent searches". */
    fun recordSearch(query: String) {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return
        val current = _recentSearches.value
        // FIX: a pause mid-word ("kit" ... "kitchen") must not leave both in the list, so a term
        // that just extends the newest one replaces it.
        val withoutPartial =
            if (current.isNotEmpty() && cleaned.startsWith(current.first(), ignoreCase = true)) current.drop(1)
            else current
        _recentSearches.value = (listOf(cleaned) + withoutPartial)
            .distinctBy { it.lowercase() }
            .take(MAX_RECENT_SEARCHES)
        searchPrefs.edit().putString("recent_searches", _recentSearches.value.joinToString("\n")).apply()
    }

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    fun storagesInRoom(roomId: Long) = repository.storagesInRoom(roomId)

    fun itemFlow(itemId: Long) = repository.itemFlow(itemId)

    suspend fun itemById(itemId: Long): ItemWithLocation? = repository.itemById(itemId)

    fun search(query: String) = repository.search(query)

    // ---------------------------------------------------------------- profile

    fun updateUsername(username: String) {
        viewModelScope.launch { repository.updateProfileUsername(username) }
    }

    fun updateProfilePhoto(photoPath: String) {
        viewModelScope.launch { repository.updateProfilePhoto(photoPath) }
    }

    // ---------------------------------------------------------------- add-item flow

    fun startItemDraft() {
        _itemDraft.value = ItemDraft()
    }

    fun setDraftPhoto(path: String?) {
        _itemDraft.value = _itemDraft.value.copy(photoPath = path)
    }

    fun setDraftName(name: String) {
        _itemDraft.value = _itemDraft.value.copy(name = name)
    }

    fun setDraftLocation(roomId: Long?, storageId: Long?, note: String) {
        _itemDraft.value = _itemDraft.value.copy(roomId = roomId, storageId = storageId, note = note)
    }

    /**
     * Saves the draft assembled across the add-item steps and prepares the confirmation.
     *
     * Returns false — without calling [onSaved] — when there is nothing to save, so the caller can
     * release its double-tap guard instead of leaving the button disabled forever.
     */
    fun saveDraftItem(onSaved: () -> Unit = {}): Boolean {
        val draft = _itemDraft.value
        if (draft.name.isBlank()) return false
        saveItem(
            itemName = draft.name,
            roomId = draft.roomId,
            storageId = draft.storageId,
            photoPath = draft.photoPath,
            description = draft.note,
            onSaved = {
                _itemDraft.value = ItemDraft()
                onSaved()
            }
        )
        return true
    }

    // ---------------------------------------------------------------- writes

    /**
     * Saves a room, and optionally the storage the user chained on from the Add Room screen.
     * [onSaved] fires once the confirmation summary is ready.
     */
    fun saveRoom(
        roomName: String,
        roomPhoto: String?,
        storageName: String? = null,
        storagePhoto: String? = null,
        onSaved: (roomId: Long, storageId: Long?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val roomId = repository.saveRoom(roomName, roomPhoto)
            val storage = storageName?.trim()?.takeIf { it.isNotEmpty() }
            val storageId = if (storage != null) {
                repository.saveStorage(roomId, storage, storagePhoto)
            } else {
                null
            }
            _pendingRoom.value = null
            _lastSaved.value = SavedSummary(
                heading = if (storage != null) "ROOM & STORAGE  SAVED!" else "ROOM SAVED",
                categoryLabel = if (storage != null) "ROOM & STORAGE" else "ROOM",
                roomName = roomName.trim().uppercase(),
                storageName = storage?.uppercase(),
                primaryPhoto = roomPhoto,
                secondaryPhoto = storagePhoto,
                paired = storage != null
            )
            onSaved(roomId, storageId)
        }
    }

    fun saveStorage(
        roomId: Long,
        storageName: String,
        storagePhoto: String?,
        onSaved: (storageId: Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val storageId = repository.saveStorage(roomId, storageName, storagePhoto)
            val room = repository.roomById(roomId)
            _lastSaved.value = SavedSummary(
                heading = "STORAGE SAVED!",
                categoryLabel = "STORAGE",
                roomName = room?.name?.uppercase(),
                storageName = storageName.trim().uppercase(),
                primaryPhoto = room?.photoPath,
                secondaryPhoto = storagePhoto,
                paired = true
            )
            onSaved(storageId)
        }
    }

    fun saveItem(
        itemName: String,
        roomId: Long?,
        storageId: Long?,
        photoPath: String?,
        description: String? = null,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveItem(itemName, roomId, storageId, photoPath, description)
            _lastSaved.value = SavedSummary(
                heading = "ITEM SAVED!",
                categoryLabel = itemName.trim().uppercase(),
                roomName = roomId?.let { repository.roomById(it)?.name?.uppercase() },
                storageName = storageId?.let { repository.storageById(it)?.name?.uppercase() },
                primaryPhoto = photoPath
            )
            onSaved()
        }
    }

    fun updateItemPhoto(itemId: Long, photoPath: String) {
        viewModelScope.launch { repository.updateItemPhoto(itemId, photoPath) }
    }

    fun updateItem(
        itemId: Long,
        name: String,
        description: String?,
        photoPath: String?,
        roomId: Long?,
        storageId: Long?,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateItem(itemId, name, description, photoPath, roomId, storageId)
            onUpdated()
        }
    }

    fun deleteItem(itemId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.archiveItem(itemId)
            onDeleted()
        }
    }

    /** Called when ADD STORAGE is tapped on Add Room, so Add Storage knows to chain. */
    fun startStorageForNewRoom(name: String, photoPath: String?) {
        _pendingRoom.value = RoomDraft(name, photoPath)
    }

    fun clearPendingRoom() {
        _pendingRoom.value = null
    }

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch { repository.archiveRoom(room) }
    }

    fun updateRoom(roomId: Long, name: String, photoPath: String?, onUpdated: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateRoom(roomId, name, photoPath)
            onUpdated()
        }
    }

    fun deleteStorage(storage: StorageEntity, archiveItems: Boolean = true, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.archiveStorage(storage, archiveItems)
            onDeleted()
        }
    }

    fun updateStorage(storageId: Long, name: String, photoPath: String?, roomId: Long, onUpdated: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateStorage(storageId, name, photoPath, roomId)
            onUpdated()
        }
    }

    fun clearLastSaved() {
        _lastSaved.value = null
    }

    fun toggleFavorite(item: ItemWithLocation) {
        viewModelScope.launch { repository.setFavorite(item.item.id, !item.item.isFavorite) }
    }

    fun setFavorite(itemId: Long, isFavorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(itemId, isFavorite) }
    }

    /** "I FOUND IT!" on the detail screen. */
    fun markFound(itemId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.markFound(itemId)
            onDone()
        }
    }

    // ---------------------------------------------------------------- archive

    suspend fun checkItemRestoreStatus(itemId: Long): RestoreCheckResult =
        repository.checkItemRestoreStatus(itemId)

    suspend fun checkStorageRestoreStatus(storageId: Long): RestoreCheckResult =
        repository.checkStorageRestoreStatus(storageId)

    fun restoreItem(itemId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.restoreItem(itemId)
            onDone()
        }
    }

    fun restoreItemUnassigned(itemId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.restoreItemUnassigned(itemId)
            onDone()
        }
    }

    fun permanentlyDeleteItem(itemId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.permanentlyDeleteItem(itemId)
            onDone()
        }
    }

    fun restoreRoom(
        roomId: Long,
        restoreStorage: Boolean = true,
        restoreItems: Boolean = true,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.restoreRoom(roomId, restoreStorage, restoreItems)
            onDone()
        }
    }

    fun permanentlyDeleteRoom(room: RoomEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.permanentlyDeleteRoom(room)
            onDone()
        }
    }

    fun restoreStorage(storageId: Long, restoreItems: Boolean = true, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.restoreStorage(storageId, restoreItems)
            onDone()
        }
    }

    fun permanentlyDeleteStorage(storage: StorageEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.permanentlyDeleteStorage(storage)
            onDone()
        }
    }
}