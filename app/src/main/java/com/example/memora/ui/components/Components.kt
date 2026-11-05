package com.example.memora.ui.components
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.theme.*

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import android.content.res.Configuration
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Modifier.photoShadow(
    elevation: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(10.dp)
): Modifier {
    val isDark = isSystemInDarkTheme()
    return this.shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = if (isDark) Color.White.copy(alpha = 0.55f) else Color.Black,
        spotColor = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black
    )
}

@Composable
fun MemoraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = SubTextGray, fontSize = 13.sp) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldCream,
            unfocusedContainerColor = FieldCream,
            focusedBorderColor = DarkGreen,
            unfocusedBorderColor = CardStroke
        ),
        modifier = modifier
    )
}

/**
 * The rounded search field.
 *
 * Pass [openSearchOnTap] on screens whose search bar is a shortcut into the search page rather
 * than a field of its own (Home, All Items): the field goes read-only and a tap anywhere except
 * the mic opens the search screen, instead of raising a keyboard for a field that does nothing.
 */
@Composable
fun MemoraSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    onMicClick: () -> Unit = {},
    openSearchOnTap: (() -> Unit)? = null
) {
    Box(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = if (isSystemInDarkTheme()) Color(0xFFB0D2C8) else Color.DarkGray,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = DarkGreen,
                    modifier = Modifier.clickable { onSearch() }
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = DarkGreen,
                    modifier = Modifier.clickable { onMicClick() }
                )
            },
            singleLine = true,
            readOnly = openSearchOnTap != null,
            // Submitting from the keyboard's Search key is what most people reach for; without this
            // the only way to run a search was tapping the small magnifier icon.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FieldCream,
                unfocusedContainerColor = FieldCream,
                focusedBorderColor = DarkGreen,
                unfocusedBorderColor = CardStroke,
                focusedTextColor = DarkGreen,
                unfocusedTextColor = DarkGreen
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (openSearchOnTap != null) {
            // Stops short of the trailing mic so voice search stays reachable.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 52.dp)
                    .clickable { openSearchOnTap() }
            )
        }
    }
}

/**
 * A social sign-in badge. [onClick] is optional: with nothing wired up the badge is drawn without
 * a ripple, so it reads as decoration rather than a button that silently does nothing.
 */
@Composable
fun SocialIconCircle(iconRes: Int, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * FIX (landscape): true while the device is held sideways. Every landscape-only layout tweak keys
 * off this, so portrait keeps exactly the layout it always had.
 */
@Composable
@ReadOnlyComposable
fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/**
 * FIX (landscape): the gap kept above a screen's title. Portrait keeps its fixed [portrait] value;
 * sideways the status bar is much shorter, so only the status bar plus a small gap is kept.
 */
@Composable
fun screenTopPadding(portrait: Dp = 52.dp): Dp {
    if (!isLandscape()) return portrait
    val statusBarTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    return statusBarTop + 8.dp
}

/** FIX (landscape): the gap under a bottom button — small sideways, where height is scarce. */
@Composable
fun screenBottomPadding(portrait: Dp): Dp = if (isLandscape()) 8.dp else portrait

/** Which tab of [MemoraBottomNavBar] is currently showing. */
enum class BottomNavTab { HOME, ALL_ITEMS, HISTORY, PROFILE }

/**
 * The shared bottom navigation bar used by Home, All Items, History and Profile, with the
 * center "+" button for the add flow. [selectedTab] controls which icon is highlighted.
 */
@Composable
fun MemoraBottomNavBar(
    selectedTab: BottomNavTab,
    onHomeClick: () -> Unit,
    onAllItemsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp))
                .background(FieldCream)
                .border(2.dp, DarkGreen, RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp))
                .padding(horizontal = 16.dp, vertical = if (isLandscape()) 6.dp else 13.dp)
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "HOME",
                selected = selectedTab == BottomNavTab.HOME,
                onClick = onHomeClick,
                modifier = Modifier.weight(1f)
            )
            BottomNavItem(
                icon = Icons.Default.List,
                label = "ALL ITEMS",
                selected = selectedTab == BottomNavTab.ALL_ITEMS,
                onClick = onAllItemsClick,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
            BottomNavItem(
                icon = Icons.Default.History,
                label = "HISTORY",
                selected = selectedTab == BottomNavTab.HISTORY,
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f)
            )
            BottomNavItem(
                icon = Icons.Default.Person,
                label = "PROFILE",
                selected = selectedTab == BottomNavTab.PROFILE,
                onClick = onProfileClick,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = if (isLandscape()) (-16).dp else (-22).dp)
                .size(if (isLandscape()) 56.dp else 80.dp)
                .clip(CircleShape)
                .background(DarkGreen)
                .padding(2.dp)
                .clip(CircleShape)
                .background(FieldCream)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = DarkGreen, modifier = Modifier.size(if (isLandscape()) 30.dp else 42.dp))
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // FIX: in dark mode DarkGreen and SubTextGray are the same mint, so the current tab never
    // stood out. Dark mode only: the selected tab is highlighted in amber; light mode is unchanged.
    val selectedColor = if (isSystemInDarkTheme()) AccentBrown else DarkGreen

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) selectedColor else SubTextGray,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) selectedColor else SubTextGray
        )
    }
}

/** A row of tappable filter chips — used by All Items and History to narrow the list. */
@Composable
fun FilterChipRow(
    labels: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(labels) { label ->
            val isSelected = label == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) PrimaryButtonContainer else FieldCream)
                    .border(1.dp, DarkGreen, RoundedCornerShape(20.dp))
                    .clickable { onSelect(label) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else DarkGreen
                )
            }
        }
    }
}

/** A colored tile used in place of a real photo, sized and rounded by the caller. */
@Composable
fun PlaceholderPhoto(
    color: Color,
    cornerRadius: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(color)
    )
}

/** A two-line item card for the All Items / Favorites grids. */
@Composable
fun GridItemCard(
    title: String,
    location: String,
    color: Color,
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
        PlaceholderPhoto(
            color = color,
            cornerRadius = 10,
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = location,
            fontSize = 11.sp,
            color = SubTextGray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}