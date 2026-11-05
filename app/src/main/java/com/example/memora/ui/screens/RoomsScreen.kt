package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.memora.data.RoomEntity
import com.example.memora.data.RoomWithCount

/**
 * "MY ROOMS" (Figma: ROOMS1 / ROOMS2 / ROOMS3) — the list, the delete confirmation, and the
 * deleted confirmation.
 *
 * Profile > MY ROOMS used to open the *Add* Room form, so there was no way to see or remove the
 * rooms that already existed.
 */
@Composable
fun RoomsScreen(
    rooms: List<RoomWithCount>,
    onBackClick: () -> Unit = {},
    onAddRoomClick: () -> Unit = {},
    onEditRoomClick: (RoomEntity) -> Unit = {},
    onDeleteRoom: (RoomEntity) -> Unit = {}
) {
    var pendingDelete by remember { mutableStateOf<RoomEntity?>(null) }
    var deletedName by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_stripes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = screenTopPadding())
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DarkGreen,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBackClick() }
                )

                Text(
                    text = "MY ROOMS",
                    color = AccentBrown,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (rooms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No rooms yet.", color = SubTextGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(rooms, key = { it.room.id }) { entry ->
                        ManageRow(
                            title = entry.room.name.uppercase(),
                            subtitle = "${entry.itemCount} items",
                            photoPath = entry.room.photoPath,
                            onEditClick = { onEditRoomClick(entry.room) },
                            onDeleteClick = { pendingDelete = entry.room }
                        )
                    }
                }
            }

            MemoraOutlinedButton(
                text = "ADD ROOM",
                onClick = onAddRoomClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = screenBottomPadding(28.dp))
            )
        }
    }

    pendingDelete?.let { room ->
        MemoraConfirmCard(
            message = "ARE YOU SURE YOU WANT TO DELETE \"${room.name.uppercase()}\"? " +
                    "IT WILL MOVE TO THE ARCHIVE ALONG WITH EVERYTHING STORED IN IT, " +
                    "AND CAN BE RESTORED LATER.",
            onNo = { pendingDelete = null },
            onYes = {
                onDeleteRoom(room)
                pendingDelete = null
                deletedName = room.name.uppercase()
            }
        )
    }

    deletedName?.let {
        MemoraResultCard(message = "ROOM ARCHIVED!", onDone = { deletedName = null })
    }
}

/** One row of the My Rooms / My Storage lists: thumbnail, name, count, and edit/delete affordances.
 *  [onEditClick] is null on rows that have no edit destination yet (My Storage), which hides the
 *  pencil rather than showing one that does nothing. */
@Composable
fun ManageRow(
    title: String,
    subtitle: String,
    photoPath: String?,
    onDeleteClick: () -> Unit,
    onEditClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, DarkGreen, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .photoShadow(elevation = 4.dp, shape = RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(PlaceholderSand)
        ) {
            if (photoPath != null) {
                AsyncImage(
                    model = rememberPhotoModel(photoPath),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
            Text(text = subtitle, fontSize = 11.sp, color = SubTextGray)
        }

        if (onEditClick != null) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit $title",
                tint = DarkGreen,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onEditClick() }
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete $title",
            tint = Color(0xFFE53935),
            modifier = Modifier
                .size(24.dp)
                .clickable { onDeleteClick() }
        )
    }
}