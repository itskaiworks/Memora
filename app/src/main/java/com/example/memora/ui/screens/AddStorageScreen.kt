package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.memora.data.RoomEntity

/**
 * Creates a storage inside a room, in one of two modes:
 *
 *  - chained from Add Room ([pendingRoom] set) — the room is already decided, so there is no room
 *    picker and SAVE writes the room and the storage together;
 *  - standalone — the user picks an existing room from the dropdown, or jumps to Add Room.
 */
@Composable
fun AddStorageScreen(
    onBackClick: () -> Unit,
    onSaved: (roomId: Long, storageId: Long?) -> Unit,
    onAddRoom: () -> Unit,
    viewModel: MemoraViewModel,
    /**
     * Set when this screen was opened from the add-item flow's "+" shortcut: the item is already
     * headed for a particular room, so offering a room picker here would let the user create the
     * storage somewhere the item will never look for it.
     */
    presetRoomId: Long? = null
) {
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val pendingRoom by viewModel.pendingRoom.collectAsStateWithLifecycle()

    var storageName by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var selectedRoom by remember { mutableStateOf<RoomEntity?>(null) }
    var showPhotoSources by remember { mutableStateOf(false) }
    // Guards against a fast double-tap writing the storage twice.
    var saving by remember { mutableStateOf(false) }

    val photoPicker = rememberPhotoPicker { path -> photoPath = path }

    val presetRoom = rooms.firstOrNull { it.id == presetRoomId }
    LaunchedEffect(presetRoom) {
        if (presetRoom != null) selectedRoom = presetRoom
    }

    val chained = pendingRoom != null
    val locked = chained || presetRoom != null
    val canSave = storageName.isNotBlank() && (chained || selectedRoom != null) && !saving

    if (showPhotoSources) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSources = false },
            onTakePhoto = { photoPicker.takePhoto() },
            onChooseFromGallery = { photoPicker.chooseFromGallery() }
        )
    }

    AddFlowScaffold(title = "ADD STORAGE", onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            PhotoWell(
                photoPath = photoPath,
                contentDescription = "Storage photo",
                onClick = { showPhotoSources = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(28.dp))

            MemoraNameField(
                value = storageName,
                onValueChange = { storageName = it },
                placeholder = "Storage Name...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (chained) {
                // The room is already settled — just show which one this storage is going into.
                RoomBeingCreatedNote(roomName = pendingRoom!!.name, prefix = "IN NEW ROOM:")
            } else if (presetRoom != null) {
                RoomBeingCreatedNote(roomName = presetRoom.name, prefix = "IN ROOM:")
            } else {
                MemoraDropdown(
                    label = "SELECT ROOM",
                    options = rooms,
                    selected = selectedRoom,
                    optionLabel = { it.name },
                    onSelect = { selectedRoom = it },
                    emptyHint = "No rooms yet — add one first",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            SuggestionsSection(
                suggestions = Suggestions.STORAGES,
                onSuggestionClick = { storageName = it }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        val saveStorage: () -> Unit = {
            saving = true
            val draft = pendingRoom
            if (draft != null) {
                viewModel.saveRoom(
                    roomName = draft.name,
                    roomPhoto = draft.photoPath,
                    storageName = storageName,
                    storagePhoto = photoPath,
                    onSaved = onSaved
                )
            } else {
                val room = selectedRoom
                if (room == null) {
                    saving = false
                } else {
                    viewModel.saveStorage(
                        roomId = room.id,
                        storageName = storageName,
                        storagePhoto = photoPath,
                        onSaved = { storageId -> onSaved(room.id, storageId) }
                    )
                }
            }
        }

        if (locked) {
            MemoraOutlinedButton(
                text = "SAVE",
                enabled = canSave,
                onClick = saveStorage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                MemoraOutlinedButton(
                    text = "ADD ROOM",
                    onClick = onAddRoom,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                MemoraPrimaryButton(
                    text = "SAVE",
                    enabled = canSave,
                    onClick = saveStorage,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RoomBeingCreatedNote(roomName: String, prefix: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "$prefix  ${roomName.uppercase()}",
            color = AccentBrown,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
