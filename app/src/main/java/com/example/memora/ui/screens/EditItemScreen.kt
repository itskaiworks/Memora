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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.memora.data.ItemWithLocation
import com.example.memora.data.RoomEntity
import com.example.memora.data.StorageWithRoom


/** Which of the edit screen's four overlays is showing, if any (Figma: the five EDIT PAGE frames). */
private enum class EditStage { FORM, CONFIRM_SAVE, CONFIRM_DELETE, SAVED, DELETED }

/**
 * "EDIT PAGE" — change an item's note and where it is filed, retake its photo, or delete it.
 *
 * Both destructive-ish actions go through the confirmation the mockups show, and each ends on its
 * own success card rather than silently popping the back stack.
 *
 * NOTE: retaking a photo here only updates the on-screen preview for now. [onSaveConfirmed] does
 * not yet carry a photoPath, and the underlying DAO update query doesn't write one either — both
 * need a small follow-on change before a retaken photo actually persists on Save.
 */
@Composable
fun EditItemScreen(
    entry: ItemWithLocation,
    rooms: List<RoomEntity>,
    allStorages: List<StorageWithRoom>,
    onBackClick: () -> Unit,
    onSaveConfirmed: (name: String, note: String, photoPath: String?, roomId: Long?, storageId: Long?) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDoneAfterSave: () -> Unit,
    onDoneAfterDelete: () -> Unit,
    onAddRoomClick: () -> Unit = {},
    onAddStorageClick: (roomId: Long?) -> Unit = {}
) {
    val item = entry.item

    // Saveable (and the room/storage kept as ids) so what has been typed or picked here survives a
    // trip out to Add Room / Add Storage via the "+" buttons and back.
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var note by rememberSaveable(item.id) { mutableStateOf(item.description.orEmpty()) }
    var photoPath by rememberSaveable(item.id) { mutableStateOf(item.photoPath) }
    var selectedRoomId by rememberSaveable(item.id) { mutableStateOf(item.roomId) }
    var selectedStorageId by rememberSaveable(item.id) { mutableStateOf(item.storageId) }

    // Looked up from the live lists, so a room or storage that has only just been created (or that
    // arrives a frame after the item on a cold start) is picked up without any extra effect.
    val selectedRoom = rooms.firstOrNull { it.id == selectedRoomId }
    val selectedStorage = allStorages.firstOrNull { it.storage.id == selectedStorageId }?.storage

    var stage by remember { mutableStateOf(EditStage.FORM) }
    var showPhotoSources by remember { mutableStateOf(false) }

    val photoPicker = rememberPhotoPicker { path -> photoPath = path }

    val storagesInRoom = remember(allStorages, selectedRoom) {
        allStorages.filter { it.storage.roomId == selectedRoom?.id }.map { it.storage }
    }

    if (showPhotoSources) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSources = false },
            onTakePhoto = { photoPicker.takePhoto() },
            onChooseFromGallery = { photoPicker.chooseFromGallery() }
        )
    }

    AddFlowScaffold(title = "EDIT ITEM", onBackClick = onBackClick) {
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
                        contentDescription = item.name,
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
                placeholder = "Item Name...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            EditSelectorRow(
                label = "SELECT ROOM",
                options = rooms,
                selected = selectedRoom,
                optionLabel = { it.name },
                onSelect = {
                    selectedRoomId = it.id
                    selectedStorageId = null
                },
                emptyHint = "No rooms yet",
                onAddClick = onAddRoomClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            EditSelectorRow(
                label = "SELECT STORAGE",
                options = storagesInRoom,
                selected = selectedStorage,
                optionLabel = { it.name },
                onSelect = { selectedStorageId = it.id },
                emptyHint = if (selectedRoom == null) "Pick a room first" else "No storage in this room",
                onAddClick = { onAddStorageClick(selectedRoomId) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "NOTE",
                color = AccentBrown,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = {
                    Text(
                        "This item is...",
                        color = if (isSystemInDarkTheme()) DarkGreen.copy(alpha = 0.7f) else AccentBrown.copy(alpha = 0.6f)
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedBorderColor = if (isSystemInDarkTheme()) CardStroke else AccentBrown,
                    unfocusedBorderColor = if (isSystemInDarkTheme()) CardStroke else AccentBrown,
                    focusedTextColor = if (isSystemInDarkTheme()) DarkGreen else AccentBrown,
                    unfocusedTextColor = if (isSystemInDarkTheme()) DarkGreen else AccentBrown
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        MemoraPrimaryButton(
            text = "SAVE CHANGES",
            enabled = name.isNotBlank(),
            onClick = { stage = EditStage.CONFIRM_SAVE },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        MemoraOutlinedButton(
            text = "DELETE ITEM",
            onClick = { stage = EditStage.CONFIRM_DELETE },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = screenBottomPadding(28.dp))
        )
    }

    when (stage) {
        EditStage.FORM -> Unit

        EditStage.CONFIRM_SAVE -> MemoraConfirmCard(
            message = "ARE YOU SURE YOU WANT TO EDIT THIS ITEM?",
            onNo = { stage = EditStage.FORM },
            onYes = {
                onSaveConfirmed(name, note, photoPath, selectedRoom?.id, selectedStorage?.id)
                stage = EditStage.SAVED
            }
        )

        EditStage.CONFIRM_DELETE -> MemoraConfirmCard(
            message = "ARE YOU SURE YOU WANT TO DELETE THIS ITEM? " +
                    "IT WILL MOVE TO THE ARCHIVE AND CAN BE RESTORED LATER.",
            onNo = { stage = EditStage.FORM },
            onYes = {
                onDeleteConfirmed()
                stage = EditStage.DELETED
            }
        )

        EditStage.SAVED -> MemoraResultCard(message = "ITEM EDITED!", onDone = onDoneAfterSave)

        EditStage.DELETED -> MemoraResultCard(message = "ITEM ARCHIVED!", onDone = onDoneAfterDelete)
    }
}

/** White pill overlaid on the photo, matching the reference mockup's "RETAKE PHOTO" tag. */
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

/** A [MemoraDropdown] with the round "+" shortcut beside it (same look as the Add Item flow). */
@Composable
private fun <T> EditSelectorRow(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    emptyHint: String,
    onAddClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        MemoraDropdown(
            label = label,
            options = options,
            selected = selected,
            optionLabel = optionLabel,
            onSelect = onSelect,
            emptyHint = emptyHint,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(FieldCream)
                .border(1.dp, DarkGreen, CircleShape)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new",
                tint = DarkGreen,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}