package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import kotlin.math.roundToInt
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.memora.data.ItemWithLocation
import com.example.memora.data.RoomWithCount

/**
 * The home screen. Everything on it now comes from the database, so anything saved through the
 * add flow shows up here immediately — previously these were three hardcoded sample lists, which
 * made a successful save look like it had done nothing.
 */
@Composable
fun HomeScreen(
    recentlyAdded: List<ItemWithLocation>,
    favorites: List<ItemWithLocation>,
    rooms: List<RoomWithCount>,
    onItemClick: (Long) -> Unit = {},
    onToggleFavorite: (ItemWithLocation) -> Unit = {},
    onAddClick: () -> Unit = {},
    onRoomClick: (RoomWithCount) -> Unit = {},
    onAllItemsClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onVoiceSearchClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    // FIX: the "MEMORA" row stays pinned at the top in both orientations. In portrait the search
    // field is pinned too; in landscape (where height is scarce) only the search field scrolls up
    // and away with the list.
    val landscape = isLandscape()

    val titleRow: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "MEMORA",
                color = DarkGreen,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Image(
                painter = painterResource(id = R.drawable.logo_memora),
                contentDescription = "Memora",
                modifier = Modifier.size(48.dp)
            )
        }
    }

    val searchField: @Composable () -> Unit = {
        MemoraSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search for an item or a room.",
            onSearch = onSearchClick,
            onMicClick = onVoiceSearchClick,
            openSearchOnTap = onSearchClick,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Landscape only: how far the search field has been pushed up (0 down to its own height).
    // Scrolling the list up collapses it first; it returns once the list is back at its top.
    var collapsedOffsetPx by remember { mutableStateOf(0f) }
    val collapsibleHeightPx = remember { floatArrayOf(0f) }
    val collapseConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                val newOffset = (collapsedOffsetPx + available.y).coerceIn(-collapsibleHeightPx[0], 0f)
                val used = newOffset - collapsedOffsetPx
                collapsedOffsetPx = newOffset
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f) return Offset.Zero
                val newOffset = (collapsedOffsetPx + available.y).coerceIn(-collapsibleHeightPx[0], 0f)
                val used = newOffset - collapsedOffsetPx
                collapsedOffsetPx = newOffset
                return Offset(0f, used)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_stripes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (landscape) Modifier.nestedScroll(collapseConnection) else Modifier)
        ) {
            if (landscape) {
                // Pinned: MEMORA + logo.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = screenTopPadding())
                ) {
                    titleRow()
                }

                // Collapsible: the search field scrolls up and away with the list.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                            )
                            collapsibleHeightPx[0] = placeable.height.toFloat()
                            val offset = collapsedOffsetPx.coerceIn(-placeable.height.toFloat(), 0f)
                            layout(
                                placeable.width,
                                (placeable.height + offset).roundToInt().coerceAtLeast(0)
                            ) {
                                placeable.placeRelative(0, offset.roundToInt())
                            }
                        }
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    searchField()
                }
            } else {
                // Portrait: MEMORA + logo and the search field both stay put while the list scrolls.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 52.dp)
                ) {
                    titleRow()
                    Spacer(modifier = Modifier.height(10.dp))
                    searchField()
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = if (isLandscape()) {
                    PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 24.dp)
                } else {
                    PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 52.dp)
                },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        SectionHeader("RECENTLY ADDED")
                        Spacer(modifier = Modifier.height(10.dp))
                        if (recentlyAdded.isEmpty()) {
                            EmptyHint("Nothing yet — tap + to add your first item.")
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(recentlyAdded, key = { it.item.id }) { entry ->
                                    ItemThumbnail(
                                        photoPath = entry.item.photoPath,
                                        onClick = { onItemClick(entry.item.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader("FAVORITE ITEMS")
                        Spacer(modifier = Modifier.height(8.dp))
                        if (favorites.isEmpty()) {
                            EmptyHint("Tap the heart on an item to keep it here.")
                        }
                    }
                }

                items(favorites, key = { it.item.id }) { entry ->
                    FavoriteItemRow(
                        entry = entry,
                        onClick = { onItemClick(entry.item.id) },
                        onToggleFavorite = { onToggleFavorite(entry) },
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item {
                    Column {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionHeader("ROOMS")
                        Spacer(modifier = Modifier.height(10.dp))
                        if (rooms.isEmpty()) {
                            EmptyHint("No rooms yet — add one from the + button.")
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(rooms, key = { it.room.id }) { entry ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { onRoomClick(entry) }
                                    ) {
                                        RoomThumbnail(photoPath = entry.room.photoPath)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = entry.room.name.uppercase(),
                                            color = DarkGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${entry.itemCount} items",
                                            color = SubTextGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            MemoraBottomNavBar(
                selectedTab = BottomNavTab.HOME,
                onHomeClick = {},
                onAllItemsClick = onAllItemsClick,
                onHistoryClick = onHistoryClick,
                onProfileClick = onProfileClick,
                onAddClick = onAddClick
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = AccentBrown,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(text = text, color = SubTextGray, fontSize = 12.sp)
}

@Composable
private fun ItemThumbnail(photoPath: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 104.dp, height = 92.dp)
            .photoShadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(PlaceholderSand)
            .clickable { onClick() }
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = rememberPhotoModel(photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun RoomThumbnail(photoPath: String?) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .photoShadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(PlaceholderCamel)
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = rememberPhotoModel(photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** "KITCHEN | TOP DRAWER", or just the room, or a nudge when the item is not filed anywhere. */
fun locationLabel(entry: ItemWithLocation): String = when {
    entry.roomName == null -> "UNFILED"
    entry.storageName == null -> entry.roomName.uppercase()
    else -> "${entry.roomName.uppercase()}  |  ${entry.storageName.uppercase()}"
}

@Composable
private fun FavoriteItemRow(
    entry: ItemWithLocation,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, DarkGreen, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .photoShadow(elevation = 4.dp, shape = RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(PlaceholderSand)
        ) {
            entry.item.photoPath?.let {
                AsyncImage(
                    model = rememberPhotoModel(it),
                    contentDescription = entry.item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.item.name.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            Text(text = locationLabel(entry), fontSize = 11.sp, color = SubTextGray)
        }

        Icon(
            imageVector = if (entry.item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Favorite",
            tint = if (isSystemInDarkTheme()) DarkGreen else if (entry.item.isFavorite) Color(0xFF3A3A3A) else SubTextGray,
            modifier = Modifier
                .size(22.dp)
                .clickable { onToggleFavorite() }
        )
    }
}