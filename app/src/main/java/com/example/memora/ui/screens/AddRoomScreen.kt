package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Creates a room. SAVE stores it on its own; ADD STORAGE carries the half-finished room into
 * [AddStorageScreen] so both get written together and confirmed as one.
 */
@Composable
fun AddRoomScreen(
    onBackClick: () -> Unit,
    onSaved: (roomId: Long, storageId: Long?) -> Unit,
    onAddStorage: (roomName: String, roomPhoto: String?) -> Unit,
    viewModel: MemoraViewModel
) {
    var roomName by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var showPhotoSources by remember { mutableStateOf(false) }
    // Guards against a fast double-tap writing the room twice and pushing two confirmations.
    var saving by remember { mutableStateOf(false) }

    val photoPicker = rememberPhotoPicker { path -> photoPath = path }
    val canSave = roomName.isNotBlank() && !saving

    if (showPhotoSources) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSources = false },
            onTakePhoto = { photoPicker.takePhoto() },
            onChooseFromGallery = { photoPicker.chooseFromGallery() }
        )
    }

    AddFlowScaffold(title = "ADD ROOM", onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            PhotoWell(
                photoPath = photoPath,
                contentDescription = "Room photo",
                onClick = { showPhotoSources = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(28.dp))

            MemoraNameField(
                value = roomName,
                onValueChange = { roomName = it },
                placeholder = "Room Name...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            SuggestionsSection(
                suggestions = Suggestions.ROOMS,
                onSuggestionClick = { roomName = it }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = screenBottomPadding(32.dp))
        ) {
            MemoraOutlinedButton(
                text = "ADD STORAGE",
                enabled = canSave,
                onClick = { onAddStorage(roomName, photoPath) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            MemoraPrimaryButton(
                text = "SAVE",
                enabled = canSave,
                onClick = {
                    saving = true
                    viewModel.saveRoom(
                        roomName = roomName,
                        roomPhoto = photoPath,
                        onSaved = onSaved
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}