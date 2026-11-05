package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memora.data.StorageEntity
import com.example.memora.data.StorageWithRoom

/**
 * "MY STORAGE" (Figma: STORAGE1 / STORAGE2 / STORAGE3) — the list, the delete confirmation, and
 * the deleted confirmation. Each row names the room the storage sits in, since storage names
 * ("Top Drawer") repeat across rooms.
 */
@Composable
fun StoragesScreen(
    storages: List<StorageWithRoom>,
    onBackClick: () -> Unit = {},
    onAddStorageClick: () -> Unit = {},
    onEditStorageClick: (StorageEntity) -> Unit = {},
    onDeleteStorage: (StorageEntity, Boolean) -> Unit = { _, _ -> }
) {
    var pendingDelete by remember { mutableStateOf<StorageEntity?>(null) }
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
                    text = "MY STORAGE",
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

            if (storages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No storage yet.", color = SubTextGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(storages, key = { it.storage.id }) { entry ->
                        ManageRow(
                            title = entry.storage.name.uppercase(),
                            subtitle = "${entry.roomName?.uppercase() ?: "UNFILED"} · ${entry.itemCount} items",
                            photoPath = entry.storage.photoPath,
                            onEditClick = { onEditStorageClick(entry.storage) },
                            onDeleteClick = { pendingDelete = entry.storage }
                        )
                    }
                }
            }

            MemoraOutlinedButton(
                text = "ADD STORAGE",
                onClick = onAddStorageClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = screenBottomPadding(28.dp))
            )
        }
    }

    pendingDelete?.let { storage ->
        StorageArchiveDialog(
            storageName = storage.name,
            onDismiss = { pendingDelete = null },
            onArchiveWithItems = {
                onDeleteStorage(storage, true)
                pendingDelete = null
                deletedName = storage.name.uppercase()
            },
            onArchiveStorageOnly = {
                onDeleteStorage(storage, false)
                pendingDelete = null
                deletedName = storage.name.uppercase()
            }
        )
    }

    deletedName?.let {
        MemoraResultCard(message = "STORAGE ARCHIVED!", onDone = { deletedName = null })
    }
}