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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
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
import com.example.memora.data.ItemWithLocation
import com.example.memora.data.RestoreCheckResult
import com.example.memora.data.RoomEntity
import com.example.memora.data.RoomWithCount
import com.example.memora.data.StorageEntity
import com.example.memora.data.StorageWithRoom
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Which archived thing a pending confirmation card refers to. */
private sealed class ArchiveTarget(val label: String) {
    data class Item(val entry: ItemWithLocation) : ArchiveTarget(entry.item.name.uppercase())
    data class Room(val room: RoomEntity) : ArchiveTarget(room.name.uppercase())
    data class Storage(val storage: StorageEntity) : ArchiveTarget(storage.name.uppercase())
}

private sealed class PendingRestoreDialog {
    object RoomDeleted : PendingRestoreDialog()
    data class ConfirmNormalRestore(val target: ArchiveTarget) : PendingRestoreDialog()
    data class RoomRestoreStep1(val room: RoomEntity) : PendingRestoreDialog()
    data class RoomRestoreStep2(val room: RoomEntity, val restoreStorage: Boolean) : PendingRestoreDialog()
    data class StorageRestorePrompt(val storage: StorageEntity) : PendingRestoreDialog()
    data class StorageDeletedPrompt1(val entry: ItemWithLocation) : PendingRestoreDialog()
    data class StorageDeletedPrompt2(val entry: ItemWithLocation) : PendingRestoreDialog()
    object StorageNotSelectedNotice : PendingRestoreDialog()
}

/**
 * "ARCHIVE" — built on the same layout, cards, spacing and typography as [RoomsScreen], since that
 * is where every one of these row actions (a bordered white card, a thumbnail, a trailing icon
 * pair) already comes from. The one visual difference is the trailing icon pair itself: a Restore
 * icon stands in for the pencil, since nothing here can be edited — only brought back or removed
 * for good — and every row keeps its trash icon for a genuinely permanent delete.
 */
@Composable
fun ArchiveScreen(
    archivedItems: List<ItemWithLocation>,
    archivedRooms: List<RoomWithCount>,
    archivedStorages: List<StorageWithRoom>,
    onBackClick: () -> Unit = {},
    onRestoreItem: (Long) -> Unit = {},
    onRestoreRoom: (roomId: Long, restoreStorage: Boolean, restoreItems: Boolean) -> Unit = { _, _, _ -> },
    onRestoreStorage: (storageId: Long, restoreItems: Boolean) -> Unit = { _, _ -> },
    onRestoreItemUnassigned: (Long) -> Unit = {},
    onEditItemToRestore: (Long) -> Unit = {},
    onCheckItemRestore: suspend (Long) -> RestoreCheckResult = { RestoreCheckResult.CAN_RESTORE },
    onCheckStorageRestore: suspend (Long) -> RestoreCheckResult = { RestoreCheckResult.CAN_RESTORE },
    onPermanentlyDeleteItem: (Long) -> Unit = {},
    onPermanentlyDeleteRoom: (RoomEntity) -> Unit = {},
    onPermanentlyDeleteStorage: (StorageEntity) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var pendingDialog by remember { mutableStateOf<PendingRestoreDialog?>(null) }
    var pendingPermanentDelete by remember { mutableStateOf<ArchiveTarget?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    val isEmpty = archivedItems.isEmpty() && archivedRooms.isEmpty() && archivedStorages.isEmpty()

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
                    text = "ARCHIVE",
                    color = AccentBrown,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            ArchiveNotice(modifier = Modifier.padding(horizontal = 20.dp))

            Spacer(modifier = Modifier.height(16.dp))

            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Nothing in the Archive right now.", color = SubTextGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    item { ArchiveSectionHeader("ITEMS", archivedItems.size) }
                    if (archivedItems.isEmpty()) {
                        item { ArchiveEmptyHint("No archived items.") }
                    } else {
                        items(archivedItems, key = { "item-${it.item.id}" }) { entry ->
                            ArchiveRow(
                                title = entry.item.name.uppercase(),
                                subtitle = "${locationLabel(entry)} · Archived ${formatArchivedAt(entry.item.archivedAt)}",
                                photoPath = entry.item.photoPath,
                                onRestoreClick = {
                                    coroutineScope.launch {
                                        when (onCheckItemRestore(entry.item.id)) {
                                            RestoreCheckResult.ROOM_DELETED -> {
                                                pendingDialog = PendingRestoreDialog.RoomDeleted
                                            }
                                            RestoreCheckResult.STORAGE_DELETED -> {
                                                pendingDialog = PendingRestoreDialog.StorageDeletedPrompt1(entry)
                                            }
                                            RestoreCheckResult.CAN_RESTORE -> {
                                                pendingDialog = PendingRestoreDialog.ConfirmNormalRestore(ArchiveTarget.Item(entry))
                                            }
                                        }
                                    }
                                },
                                onDeleteClick = { pendingPermanentDelete = ArchiveTarget.Item(entry) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ArchiveSectionHeader("ROOMS", archivedRooms.size)
                    }
                    if (archivedRooms.isEmpty()) {
                        item { ArchiveEmptyHint("No archived rooms.") }
                    } else {
                        items(archivedRooms, key = { "room-${it.room.id}" }) { entry ->
                            ArchiveRow(
                                title = entry.room.name.uppercase(),
                                subtitle = "${entry.itemCount} items · Archived ${formatArchivedAt(entry.room.archivedAt)}",
                                photoPath = entry.room.photoPath,
                                onRestoreClick = {
                                    pendingDialog = PendingRestoreDialog.RoomRestoreStep1(entry.room)
                                },
                                onDeleteClick = { pendingPermanentDelete = ArchiveTarget.Room(entry.room) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ArchiveSectionHeader("STORAGE", archivedStorages.size)
                    }
                    if (archivedStorages.isEmpty()) {
                        item { ArchiveEmptyHint("No archived storage.") }
                    } else {
                        items(archivedStorages, key = { "storage-${it.storage.id}" }) { entry ->
                            ArchiveRow(
                                title = entry.storage.name.uppercase(),
                                subtitle = "${entry.roomName?.uppercase() ?: "UNFILED"} · " +
                                        "Archived ${formatArchivedAt(entry.storage.archivedAt)}",
                                photoPath = entry.storage.photoPath,
                                onRestoreClick = {
                                    coroutineScope.launch {
                                        when (onCheckStorageRestore(entry.storage.id)) {
                                            RestoreCheckResult.ROOM_DELETED -> {
                                                pendingDialog = PendingRestoreDialog.RoomDeleted
                                            }
                                            RestoreCheckResult.STORAGE_DELETED,
                                            RestoreCheckResult.CAN_RESTORE -> {
                                                pendingDialog = PendingRestoreDialog.StorageRestorePrompt(entry.storage)
                                            }
                                        }
                                    }
                                },
                                onDeleteClick = { pendingPermanentDelete = ArchiveTarget.Storage(entry.storage) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
        }
    }

    when (val dialog = pendingDialog) {
        is PendingRestoreDialog.RoomDeleted -> {
            MemoraInfoCard(
                message = "Cannot restore because the room has been deleted.",
                buttonText = "OKAY",
                onOkay = { pendingDialog = null }
            )
        }
        is PendingRestoreDialog.ConfirmNormalRestore -> {
            val target = dialog.target
            MemoraConfirmCard(
                message = "RESTORE \"${target.label}\"? IT WILL RETURN TO ITS PREVIOUS LOCATION.",
                onNo = { pendingDialog = null },
                onYes = {
                    when (target) {
                        is ArchiveTarget.Item -> onRestoreItem(target.entry.item.id)
                        is ArchiveTarget.Room -> onRestoreRoom(target.room.id, true, true)
                        is ArchiveTarget.Storage -> onRestoreStorage(target.storage.id, true)
                    }
                    pendingDialog = null
                    resultMessage = "RESTORED!"
                }
            )
        }
        is PendingRestoreDialog.RoomRestoreStep1 -> {
            MemoraConfirmCard(
                message = "DO YOU WANT TO RESTORE ALL STORAGE IN THIS ROOM TOO?",
                onNo = {
                    pendingDialog = PendingRestoreDialog.RoomRestoreStep2(dialog.room, restoreStorage = false)
                },
                onYes = {
                    pendingDialog = PendingRestoreDialog.RoomRestoreStep2(dialog.room, restoreStorage = true)
                }
            )
        }
        is PendingRestoreDialog.RoomRestoreStep2 -> {
            val message = if (dialog.restoreStorage) {
                "DO YOU WANT TO RESTORE ALL ITEMS IN THIS ROOM TOO?"
            } else {
                "DO YOU WANT TO RESTORE ALL ITEMS IN THIS ROOM?"
            }
            MemoraConfirmCard(
                message = message,
                onNo = {
                    onRestoreRoom(dialog.room.id, dialog.restoreStorage, false)
                    pendingDialog = null
                    resultMessage = "RESTORED!"
                },
                onYes = {
                    onRestoreRoom(dialog.room.id, dialog.restoreStorage, true)
                    pendingDialog = null
                    resultMessage = "RESTORED!"
                }
            )
        }
        is PendingRestoreDialog.StorageRestorePrompt -> {
            StorageRestoreDialog(
                storageName = dialog.storage.name,
                onDismiss = { pendingDialog = null },
                onRestoreWithItems = {
                    onRestoreStorage(dialog.storage.id, true)
                    pendingDialog = null
                    resultMessage = "RESTORED!"
                },
                onRestoreStorageOnly = {
                    onRestoreStorage(dialog.storage.id, false)
                    pendingDialog = null
                    resultMessage = "RESTORED!"
                }
            )
        }
        is PendingRestoreDialog.StorageDeletedPrompt1 -> {
            MemoraConfirmCard(
                message = "THE STORAGE WHERE THIS ITEM WAS PREVIOUSLY LOCATED IS NO LONGER AVAILABLE. WOULD YOU LIKE TO CHOOSE A DIFFERENT STORAGE?",
                onNo = {
                    pendingDialog = PendingRestoreDialog.StorageDeletedPrompt2(dialog.entry)
                },
                onYes = {
                    val itemId = dialog.entry.item.id
                    pendingDialog = null
                    onEditItemToRestore(itemId)
                }
            )
        }
        is PendingRestoreDialog.StorageDeletedPrompt2 -> {
            MemoraConfirmCard(
                message = "WOULD YOU LIKE TO RESTORE THIS ITEM TO THE ROOM WITHOUT ASSIGNING IT TO A SPECIFIC STORAGE?",
                onNo = {
                    pendingDialog = PendingRestoreDialog.StorageNotSelectedNotice
                },
                onYes = {
                    onRestoreItemUnassigned(dialog.entry.item.id)
                    pendingDialog = null
                    resultMessage = "RESTORED!"
                }
            )
        }
        is PendingRestoreDialog.StorageNotSelectedNotice -> {
            MemoraInfoCard(
                message = "Item was not restored because no valid storage was selected.",
                buttonText = "OKAY",
                onOkay = { pendingDialog = null }
            )
        }
        null -> {}
    }

    pendingPermanentDelete?.let { target ->
        MemoraConfirmCard(
            message = "PERMANENTLY DELETE \"${target.label}\"? THIS CANNOT BE UNDONE.",
            onNo = { pendingPermanentDelete = null },
            onYes = {
                when (target) {
                    is ArchiveTarget.Item -> onPermanentlyDeleteItem(target.entry.item.id)
                    is ArchiveTarget.Room -> onPermanentlyDeleteRoom(target.room)
                    is ArchiveTarget.Storage -> onPermanentlyDeleteStorage(target.storage)
                }
                pendingPermanentDelete = null
                resultMessage = "PERMANENTLY DELETED!"
            }
        )
    }

    resultMessage?.let { message ->
        MemoraResultCard(message = message, onDone = { resultMessage = null })
    }
}

/** The 30-day policy note at the top of the screen. */
@Composable
private fun ArchiveNotice(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FieldCream)
            .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = AccentBrown,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Items, rooms and storage here are permanently deleted after 30 days " +
                    "if they aren't restored.",
            color = AccentBrown,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun ArchiveSectionHeader(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        color = DarkGreen,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ArchiveEmptyHint(text: String) {
    Text(text = text, color = SubTextGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
}

/** Same card as [ManageRow] in RoomsScreen.kt — thumbnail, title, subtitle — but with the pencil
 *  swapped for a Restore icon, since nothing in the Archive can be edited in place. */
@Composable
private fun ArchiveRow(
    title: String,
    subtitle: String,
    photoPath: String?,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit
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

        Icon(
            imageVector = Icons.Default.Restore,
            contentDescription = "Restore $title",
            tint = DarkGreen,
            modifier = Modifier
                .size(22.dp)
                .clickable { onRestoreClick() }
        )

        Spacer(modifier = Modifier.width(14.dp))

        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Permanently delete $title",
            tint = Color(0xFFE53935),
            modifier = Modifier
                .size(24.dp)
                .clickable { onDeleteClick() }
        )
    }
}

private fun formatArchivedAt(archivedAt: Long?): String {
    if (archivedAt == null) return "recently"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(archivedAt))
}