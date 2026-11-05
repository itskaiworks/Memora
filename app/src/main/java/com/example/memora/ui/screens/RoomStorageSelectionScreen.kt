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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memora.data.RoomEntity
import com.example.memora.data.StorageEntity

/**
 * Step 3 of the add-item flow (Figma: ITEM DETAILS - ROOM & STORAGE SELECTION) — file the item.
 *
 * The "+" beside each picker jumps into Add Room / Add Storage and comes back here, which is what
 * keeps the flow from dead-ending when the user realises mid-way that the room does not exist yet.
 */
@Composable
fun RoomStorageSelectionScreen(
    photoPath: String?,
    rooms: List<RoomEntity>,
    storages: List<StorageEntity>,
    selectedRoom: RoomEntity?,
    selectedStorage: StorageEntity?,
    note: String,
    onRoomSelected: (RoomEntity) -> Unit,
    onStorageSelected: (StorageEntity) -> Unit,
    onNoteChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onAddRoomClick: () -> Unit,
    onAddStorageClick: () -> Unit,
    /** Returns false when nothing was saved, so the button re-enables itself. */
    onSaveClick: () -> Boolean
) {
    // Guards against a fast double-tap saving the item twice and stacking two confirmations.
    var saving by remember { mutableStateOf(false) }

    AddFlowScaffold(title = "ROOM & STORAGE", onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            PhotoWell(
                photoPath = photoPath,
                contentDescription = "Item photo",
                size = 160.dp,
                onClick = {},
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            SelectorRow(
                label = "SELECT ROOM",
                options = rooms,
                selected = selectedRoom,
                optionLabel = { it.name },
                onSelect = onRoomSelected,
                emptyHint = "No rooms yet — tap + to add one",
                onAddClick = onAddRoomClick
            )

            Spacer(modifier = Modifier.height(14.dp))

            SelectorRow(
                label = "SELECT STORAGE",
                options = storages,
                selected = selectedStorage,
                optionLabel = { it.name },
                onSelect = onStorageSelected,
                emptyHint = if (selectedRoom == null) "Pick a room first" else "No storage here — tap + to add one",
                onAddClick = onAddStorageClick
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "ADD NOTE (OPTIONAL)",
                color = AccentBrown,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
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

        MemoraOutlinedButton(
            text = "SAVE",
            enabled = selectedRoom != null && !saving,
            onClick = { saving = onSaveClick() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = screenBottomPadding(32.dp))
        )
    }
}

/** A [MemoraDropdown] with the round "+" shortcut beside it. */
@Composable
private fun <T> SelectorRow(
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