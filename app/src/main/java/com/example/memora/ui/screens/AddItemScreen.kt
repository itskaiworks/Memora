package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "SELECT WHAT TO ADD" — the hub the centre + button opens (Figma: ADD PAGE).
 *
 * Before this existed the + went straight to the add-item form, which made Add Room and Add
 * Storage reachable only through Profile — and left a first-time user stuck, because an item
 * cannot be filed until at least one room exists.
 */
@Composable
fun AddChooserScreen(
    onBackClick: () -> Unit = {},
    onAddItemClick: () -> Unit = {},
    onAddRoomClick: () -> Unit = {},
    onAddStorageClick: () -> Unit = {}
) {
    AddFlowScaffold(title = "SELECT WHAT TO ADD", onBackClick = onBackClick) {
        if (isLandscape()) {
            // FIX (landscape): three stacked 160dp tiles need endless scrolling; side by side they fit.
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AddChoiceTile(
                    label = "ADD ITEM",
                    imageRes = R.drawable.add_item_photo,
                    onClick = onAddItemClick,
                    tileSize = 110.dp
                )
                AddChoiceTile(
                    label = "ADD ROOM",
                    imageRes = R.drawable.add_room_photo,
                    onClick = onAddRoomClick,
                    tileSize = 110.dp
                )
                AddChoiceTile(
                    label = "ADD STORAGE",
                    imageRes = R.drawable.add_storage_photo,
                    onClick = onAddStorageClick,
                    tileSize = 110.dp
                )
            }
        } else Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 26.dp)
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            AddChoiceTile(
                label = "ADD ITEM",
                imageRes = R.drawable.add_item_photo,
                onClick = onAddItemClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            AddChoiceTile(
                label = "ADD ROOM",
                imageRes = R.drawable.add_room_photo,
                onClick = onAddRoomClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            AddChoiceTile(
                label = "ADD STORAGE",
                imageRes = R.drawable.add_storage_photo,
                onClick = onAddStorageClick
            )
        }
    }
}

@Composable
private fun AddChoiceTile(
    label: String,
    imageRes: Int,
    onClick: () -> Unit,
    tileSize: Dp = 160.dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // A round, raised button: the photo sits inside a cream ring with a green outline.
        Box(
            modifier = Modifier
                .size(tileSize)
                .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(FieldCream)
                .border(2.dp, DarkGreen, CircleShape)
                .clickable { onClick() }
                .padding(6.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = AccentBrown,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}