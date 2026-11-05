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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import com.example.memora.data.ItemWithLocation
import com.example.memora.ui.theme.MemoraTheme

// FIX: most recent searches shown (and kept), like the usual search screens.
const val MAX_RECENT_SEARCHES = 5

/**
 * "SEARCH" screen — search field, live results while typing, and recent-search chips when the
 * field is empty.
 *
 * Typing now filters in place on this same screen as each character is entered — no Enter,
 * Search-icon tap, or other confirmation needed. [query] and [results] are owned by the caller
 * (see [MemoraNavHost]) so the same live database query backs both the field and the list below
 * it. Voice Search is unaffected: it still hands off to [SearchResultsScreen] once the recognized
 * phrase comes back (see [SearchVoiceScreen]/[MemoraNavHost]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    results: List<ItemWithLocation> = emptyList(),
    // Filled from what the user has actually searched for. The eight fabricated "recent"
    // searches that used to be hardcoded here all dead-ended on "NO MATCHES".
    recentSearches: List<String> = emptyList(),
    onBackClick: () -> Unit = {},
    onResultClick: (Long) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    onMicClick: () -> Unit = {}
) {
    // FIX: typing alone never reached Recent Searches (only the keyboard Search key / magnifier
    // did). Once the user pauses on a query that actually found something, keep it.
    LaunchedEffect(query, results) {
        if (query.isNotBlank() && results.isNotEmpty()) {
            delay(800)
            onSearchSubmit(query)
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(screenTopPadding(50.dp)))

            // Top bar: back arrow + centered title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
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
                    text = "SEARCH",
                    color = DarkGreen,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            MemoraSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search for an item or a room.",
                // The keyboard's Search key / the field's own icon no longer navigate anywhere —
                // typing already shows results live — but still record the term and drop the
                // keyboard, which is what most people expect that key to do.
                onSearch = { if (query.isNotBlank()) onSearchSubmit(query) },
                onMicClick = onMicClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (query.isBlank()) {
                // FIX: only the newest few are displayed.
                val shownRecentSearches = recentSearches.take(MAX_RECENT_SEARCHES)

                if (shownRecentSearches.isNotEmpty()) {
                    Text(
                        text = "RECENT SEARCHES",
                        // FIX: amber like the SUGGESTIONS header, DARK MODE ONLY.
                        color = if (isSystemInDarkTheme()) AccentBrown else DarkGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    shownRecentSearches.forEach { term ->
                        RecentSearchChip(
                            text = term,
                            // Fills the field with the chip's term; the live results below react
                            // to that the same way they would to typing it, so no navigation here.
                            onClick = { onQueryChange(term) }
                        )
                    }
                }
            } else {
                // Live results for the query typed so far. This list is driven entirely by
                // [results], which the caller recomputes from [query] on every keystroke.
                Text(
                    text = if (results.isEmpty()) "NO MATCHES" else "${results.size} FOUND",
                    color = AccentBrown,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (results.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nothing matches that yet. Try a room or storage name.",
                            color = SubTextGray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(results, key = { it.item.id }) { entry ->
                            LiveSearchResultRow(
                                entry = entry,
                                onClick = {
                                    // FIX: opening a result also counts as a search worth keeping.
                                    onSearchSubmit(query)
                                    onResultClick(entry.item.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearchChip(text: String, onClick: () -> Unit) {
    // FIX: pill shape (same as the Suggestions chips) in both modes; the amber border and text
    // are DARK MODE ONLY — light mode keeps its original black border and green text.
    val isDark = isSystemInDarkTheme()
    val chipShape = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .clip(chipShape)
            .background(FieldCream)
            .border(
                width = if (isDark) 1.dp else 1.5.dp,
                color = if (isDark) AccentBrown else Color.Black,
                shape = chipShape
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) AccentBrown else DarkGreen,
            letterSpacing = 0.3.sp
        )
    }
}

/** One row of the type-as-you-go results — same card styling used across the app's list screens
 *  (e.g. [SearchResultsScreen]'s row), kept local here since this list has no date column. */
@Composable
private fun LiveSearchResultRow(entry: ItemWithLocation, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, DarkGreen, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(10.dp)
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.item.name.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            Text(text = locationLabel(entry), fontSize = 11.sp, color = SubTextGray)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun SearchScreenPreview() {
    MemoraTheme {
        SearchScreen()
    }
}