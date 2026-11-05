package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.memora.data.RoomEntity
import com.example.memora.data.StorageEntity

/** Which of the edit screen's four overlays is showing, if any — mirrors [EditItemScreen]'s stages. */
private enum class EditStorageStage { FORM, CONFIRM_SAVE, CONFIRM_DELETE, SAVED, DELETED }

/**
 * "EDIT STORAGE" — rename a storage, retake its photo, move it to a different room, or delete it.
 *
 * Unlike the room-side dropdown in [EditRoomDetailsScreen], "SELECT ROOM" here really does reassign
 * the storage on Save, since a storage belongs to exactly one room. Moving a storage does not touch
 * the items already filed under it, though, so an item's own "room" tag can end up out of step with
 * its storage's new room until that item is opened and re-saved.
 */
@Composable
fun EditStorageDetailsScreen(
    storage: StorageEntity,
    rooms: List<RoomEntity>,
    onBackClick: () -> Unit,
    onSaveConfirmed: (name: String, photoPath: String?, roomId: Long) -> Unit,
    onDeleteConfirmed: (archiveItems: Boolean) -> Unit,
    onAddRoomClick: () -> Unit,
    onDoneAfterSave: () -> Unit,
    onDoneAfterDelete: () -> Unit
) {
    var name by remember(storage.id) { mutableStateOf(storage.name) }
    var photoPath by remember(storage.id) { mutableStateOf(storage.photoPath) }
    var selectedRoom by remember(storage.id) {
        mutableStateOf(rooms.firstOrNull { it.id == storage.roomId })
    }
    var stage by remember { mutableStateOf(EditStorageStage.FORM) }
    var showPhotoSources by remember { mutableStateOf(false) }

    val photoPicker = rememberPhotoPicker { path -> photoPath = path }

    // The room list can arrive a frame after the storage on a cold start; pick up the storage's
    // own room once it is there, so the picker does not sit on "SELECT ROOM" unnecessarily.
    LaunchedEffect(rooms, storage.roomId) {
        if (selectedRoom == null) selectedRoom = rooms.firstOrNull { it.id == storage.roomId }
    }

    if (showPhotoSources) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSources = false },
            onTakePhoto = { photoPicker.takePhoto() },
            onChooseFromGallery = { photoPicker.chooseFromGallery() }
        )
    }

    AddFlowScaffold(title = "EDIT STORAGE", onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            val currentPhotoPath = photoPath
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape()) 220.dp else 320.dp)
                    .photoShadow(elevation = 4.dp, shape = RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(PlaceholderSand)
                    .clickable { showPhotoSources = true },
                contentAlignment = Alignment.Center
            ) {
                if (currentPhotoPath != null) {
                    AsyncImage(
                        model = rememberPhotoModel(currentPhotoPath),
                        contentDescription = storage.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(text = "NO PHOTO", color = SubTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                RetakePhotoButton(
                    onClick = { showPhotoSources = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MemoraDropdown(
                    label = "SELECT ROOM",
                    options = rooms,
                    selected = selectedRoom,
                    optionLabel = { it.name },
                    onSelect = { selectedRoom = it },
                    emptyHint = "No rooms yet",
                    modifier = Modifier.weight(1f)
                )

                AddRoomCircleButton(onClick = onAddRoomClick)
            }

            Spacer(modifier = Modifier.height(14.dp))

            MemoraNameField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Storage Name...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        MemoraPrimaryButton(
            text = "SAVE CHANGES",
            enabled = name.isNotBlank() && selectedRoom != null,
            onClick = { stage = EditStorageStage.CONFIRM_SAVE },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        MemoraOutlinedButton(
            text = "DELETE STORAGE",
            onClick = { stage = EditStorageStage.CONFIRM_DELETE },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = screenBottomPadding(28.dp))
        )
    }

    when (stage) {
        EditStorageStage.FORM -> Unit

        EditStorageStage.CONFIRM_SAVE -> MemoraConfirmCard(
            message = "ARE YOU SURE YOU WANT TO EDIT THIS STORAGE?",
            onNo = { stage = EditStorageStage.FORM },
            onYes = {
                val room = selectedRoom
                if (room != null) {
                    onSaveConfirmed(name, photoPath, room.id)
                    stage = EditStorageStage.SAVED
                } else {
                    stage = EditStorageStage.FORM
                }
            }
        )

        EditStorageStage.CONFIRM_DELETE -> StorageArchiveDialog(
            storageName = storage.name,
            onDismiss = { stage = EditStorageStage.FORM },
            onArchiveWithItems = {
                onDeleteConfirmed(true)
                stage = EditStorageStage.DELETED
            },
            onArchiveStorageOnly = {
                onDeleteConfirmed(false)
                stage = EditStorageStage.DELETED
            }
        )

        EditStorageStage.SAVED -> MemoraResultCard(message = "STORAGE EDITED!", onDone = onDoneAfterSave)

        EditStorageStage.DELETED -> MemoraResultCard(message = "STORAGE ARCHIVED!", onDone = onDoneAfterDelete)
    }
}

/** White pill overlaid on the photo — the same treatment as [EditItemScreen]'s retake button. */
@Composable
private fun RetakePhotoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(AccentBrown)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "RETAKE PHOTO",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

/** Round "+" beside the dropdown — the same shape used to jump into Add Room / Add Storage elsewhere. */
@Composable
private fun AddRoomCircleButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(FieldCream)
            .border(1.dp, DarkGreen, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add a new room",
            tint = DarkGreen,
            modifier = Modifier.size(26.dp)
        )
    }
}