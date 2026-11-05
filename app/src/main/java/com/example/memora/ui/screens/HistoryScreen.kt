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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memora.data.EventEntity
import com.example.memora.data.ItemAction
import com.example.memora.ui.theme.BadgeAdded
import com.example.memora.ui.theme.BadgeDeleted
import com.example.memora.ui.theme.BadgeEdited
import com.example.memora.ui.theme.BadgeRetrieved
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun badgeColor(action: String): Color = when (action) {
    ItemAction.ADDED.name -> BadgeAdded
    ItemAction.RETRIEVED.name, ItemAction.RESTORED.name -> BadgeRetrieved
    ItemAction.ARCHIVED.name, ItemAction.DELETED.name -> BadgeDeleted
    else -> BadgeEdited
}

private data class HistoryDateGroup(val date: String, val entries: List<EventEntity>)

/**
 * "HISTORY" — a real activity log. Every add, edit, delete and "I FOUND IT!" writes an event row,
 * so this screen reflects what the user actually did instead of a fixed sample list.
 */
@Composable
fun HistoryScreen(
    events: List<EventEntity>,
    onHomeClick: () -> Unit = {},
    onAllItemsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val filters = listOf("ALL", "ADDED", "RETRIEVED", "EDITED", "ARCHIVED", "RESTORED", "DELETED")
    var selectedFilter by remember { mutableStateOf("ALL") }

    val visibleGroups = remember(events, selectedFilter) {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        events
            .filter { selectedFilter == "ALL" || it.action == selectedFilter }
            .groupBy { dateFormat.format(Date(it.createdAt)).uppercase() }
            .map { (date, entries) -> HistoryDateGroup(date, entries) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_stripes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "HISTORY",
                color = DarkGreen,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = screenTopPadding())
            )

            Spacer(modifier = Modifier.height(14.dp))

            FilterChipRow(
                labels = filters,
                selected = selectedFilter,
                onSelect = { selectedFilter = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (visibleGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (events.isEmpty()) "Nothing has happened yet."
                        else "No $selectedFilter activity yet.",
                        color = SubTextGray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    visibleGroups.forEach { group ->
                        item(key = "date-${group.date}") {
                            Text(
                                text = group.date,
                                color = DarkGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        group.entries.forEachIndexed { index, entry ->
                            item(key = "event-${entry.id}") {
                                HistoryRow(
                                    entry = entry,
                                    isLast = index == group.entries.lastIndex
                                )
                            }
                        }
                    }
                }
            }

            MemoraBottomNavBar(
                selectedTab = BottomNavTab.HISTORY,
                onHomeClick = onHomeClick,
                onAllItemsClick = onAllItemsClick,
                onHistoryClick = {},
                onProfileClick = onProfileClick,
                onAddClick = onAddClick
            )
        }
    }
}

@Composable
private fun HistoryRow(entry: EventEntity, isLast: Boolean) {
    val timeText = remember(entry.createdAt) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.createdAt))
    }

    Row(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(52.dp)
        ) {
            Text(text = timeText, fontSize = 9.sp, color = SubTextGray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(DarkGreen)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(2.dp)
                    .background(if (isLast) Color.Transparent else CardStroke)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground)
                .border(1.dp, CardStroke, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.itemName.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
                Text(text = eventLocation(entry), fontSize = 10.sp, color = SubTextGray)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor(entry.action))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = entry.action,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private fun eventLocation(entry: EventEntity): String = when {
    entry.roomName == null -> entry.description ?: "UNFILED"
    entry.storageName == null -> entry.roomName.uppercase()
    else -> "${entry.roomName.uppercase()} - ${entry.storageName.uppercase()}"
}