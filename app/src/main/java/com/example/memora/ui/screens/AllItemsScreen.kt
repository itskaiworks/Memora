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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.memora.data.ItemWithLocation
import com.example.memora.data.RoomEntity

/**
 * "ALL ITEMS" — every saved item, filterable by room. The filter chips are built from the rooms
 * that actually exist rather than a fixed list, so a newly added room can be filtered by at once.
 */
@Composable
fun AllItemsScreen(
    items: List<ItemWithLocation>,
    rooms: List<RoomEntity>,
    /** Room to pre-select on arrival (e.g. tapped from the Home dashboard), or null for "ALL". */
    initialRoomFilter: String? = null,
    onHomeClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onItemClick: (Long) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMicClick: () -> Unit = {}
) {
    // Not keyed on initialRoomFilter: it's a one-shot seed for this screen instance. The caller
    // clears it right after this first composition (see MainActivity), and re-keying here would
    // snap the chip back to "ALL" the moment that happens, undoing the very selection it made.
    var selectedFilter by remember { mutableStateOf(initialRoomFilter?.uppercase() ?: "ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val roomFilters = remember(rooms) { listOf("ALL") + rooms.map { it.name.uppercase() } }

    // A room can be deleted while its chip is selected; fall back to ALL instead of showing nothing.
    val activeFilter = if (selectedFilter in roomFilters) selectedFilter else "ALL"

    val visibleItems = remember(items, activeFilter) {
        if (activeFilter == "ALL") items
        else items.filter { it.roomName?.uppercase() == activeFilter }
    }

    // FIX (landscape): the title, search field and filter chips used to sit above the grid and
    // left almost no room for it. Sideways only the search field scrolls away with the grid; the
    // "ALL ITEMS" title and the filter chips stay pinned (see the collapse logic below).
    val landscape = isLandscape()

    val title: @Composable ColumnScope.() -> Unit = {
        Text(
            text = "ALL ITEMS",
            color = DarkGreen,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )
    }

    val searchBlock: @Composable ColumnScope.() -> Unit = {
        MemoraSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search for an item or a room.",
            onSearch = onSearchClick,
            onMicClick = onMicClick,
            openSearchOnTap = onSearchClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
    }

    val chips: @Composable ColumnScope.() -> Unit = {
        FilterChipRow(
            labels = roomFilters,
            selected = activeFilter,
            onSelect = { selectedFilter = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
    }

    // Portrait header: exactly the original layout.
    val header: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = screenTopPadding())
        ) {
            title()
            Spacer(modifier = Modifier.height(12.dp))
            searchBlock()
            chips()
        }
    }

    // Landscape only: how far the search block has been pushed up (0 down to its own
    // height). Scrolling the grid up collapses it first; it comes back once the grid is back at
    // its top.
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
                // Pinned: the title.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = screenTopPadding()),
                    content = title
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Collapsible: the search field scrolls up and away with the grid.
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
                        .padding(horizontal = 20.dp),
                    content = searchBlock
                )

                // Pinned: the filter chips.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    content = chips
                )
            } else {
                header()
            }

            if (visibleItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (items.isEmpty()) "No items yet — tap + to add one."
                        else "Nothing filed in this room yet.",
                        color = SubTextGray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = if (landscape) GridCells.Adaptive(150.dp) else GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(visibleItems, key = { it.item.id }) { entry ->
                        ItemGridCard(entry = entry, onClick = { onItemClick(entry.item.id) })
                    }
                }
            }

            MemoraBottomNavBar(
                selectedTab = BottomNavTab.ALL_ITEMS,
                onHomeClick = onHomeClick,
                onAllItemsClick = {},
                onHistoryClick = onHistoryClick,
                onProfileClick = onProfileClick,
                onAddClick = onAddClick
            )
        }
    }
}

/** Shared by All Items and Favorites — a photo tile with the item's name and where it is filed. */
@Composable
fun ItemGridCard(
    entry: ItemWithLocation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, DarkGreen, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = entry.item.name.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = locationLabel(entry),
            fontSize = 11.sp,
            color = SubTextGray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}