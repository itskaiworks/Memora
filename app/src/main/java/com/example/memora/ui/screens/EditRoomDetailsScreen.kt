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
private enum class EditRoomStage { FORM, CONFIRM_SAVE, CONFIRM_DELETE, SAVED, DELETED }

/**
 * "EDIT ROOM" — rename a room, retake its photo, jump into Add Storage for it, or delete it.
 *
 * A room can hold many storages, so the "SELECT STORAGE" dropdown here is a look at what's already
 * filed in this room rather than an assignment — picking one doesn't change anything on Save. The
 * round "+" beside it is what actually creates a new storage for this room, via [onAddStorageClick].
 */
@Composable
fun EditRoomDetailsScreen(
    room: RoomEntity,
    storagesInRoom: List<StorageEntity>,
    onBackClick: () -> Unit,
    onSaveConfirmed: (name: String, photoPath: String?) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onAddStorageClick: () -> Unit,
    onDoneAfterSave: () -> Unit,
    onDoneAfterDelete: () -> Unit
) {
    var name by remember(room.id) { mutableStateOf(room.name) }
    var photoPath by remember(room.id) { mutableStateOf(room.photoPath) }
    var selectedStorage by remember(room.id) { mutableStateOf<StorageEntity?>(null) }
    var stage by remember { mutableStateOf(EditRoomStage.FORM) }
    var showPhotoSources by remember { mutableStateOf(false) }

    val photoPicker = rememberPhotoPicker { path -> photoPath = path }

    if (showPhotoSources) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSources = false },
            onTakePhoto = { photoPicker.takePhoto() },
            onChooseFromGallery = { photoPicker.chooseFromGallery() }
        )
    }

    AddFlowScaffold(title = "EDIT ROOM", onBackClick = onBackClick) {
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
                        contentDescription = room.name,
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

            MemoraNameField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Room Name...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MemoraDropdown(
                    label = "SELECT STORAGE",
                    options = storagesInRoom,
                    selected = selectedStorage,
                    optionLabel = { it.name },
                    onSelect = { selectedStorage = it },
                    emptyHint = "No storage in this room yet",
                    modifier = Modifier.weight(1f)
                )

                AddStorageCircleButton(onClick = onAddStorageClick)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        MemoraPrimaryButton(
            text = "SAVE CHANGES",
            enabled = name.isNotBlank(),
            onClick = { stage = EditRoomStage.CONFIRM_SAVE },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        MemoraOutlinedButton(
            text = "DELETE ROOM",
            onClick = { stage = EditRoomStage.CONFIRM_DELETE },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = screenBottomPadding(28.dp))
        )
    }

    when (stage) {
        EditRoomStage.FORM -> Unit

        EditRoomStage.CONFIRM_SAVE -> MemoraConfirmCard(
            message = "ARE YOU SURE YOU WANT TO EDIT THIS ROOM?",
            onNo = { stage = EditRoomStage.FORM },
            onYes = {
                onSaveConfirmed(name, photoPath)
                stage = EditRoomStage.SAVED
            }
        )

        EditRoomStage.CONFIRM_DELETE -> MemoraConfirmCard(
            message = "ARE YOU SURE YOU WANT TO DELETE \"${room.name.uppercase()}\"? " +
                    "IT WILL MOVE TO THE ARCHIVE ALONG WITH EVERYTHING STORED IN IT, " +
                    "AND CAN BE RESTORED LATER.",
            onNo = { stage = EditRoomStage.FORM },
            onYes = {
                onDeleteConfirmed()
                stage = EditRoomStage.DELETED
            }
        )

        EditRoomStage.SAVED -> MemoraResultCard(message = "ROOM EDITED!", onDone = onDoneAfterSave)

        EditRoomStage.DELETED -> MemoraResultCard(message = "ROOM ARCHIVED!", onDone = onDoneAfterDelete)
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
private fun AddStorageCircleButton(onClick: () -> Unit) {
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
            contentDescription = "Add storage to this room",
            tint = DarkGreen,
            modifier = Modifier.size(26.dp)
        )
    }
}