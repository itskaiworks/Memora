package com.example.memora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Light Mode defaults (preserved 100% EXACTLY as original)
val LightDarkGreen = Color(0xFF2C3B20)
val LightFieldCream = Color(0xFFFCFAF4)
val LightTitleGray = Color(0xFFB8B8B8)
val LightSubTextGray = Color(0xFF9C9C9C)
val LightAccentBrown = Color(0xFF5C4A32)
val LightCardStroke = Color(0xFFE3DFD3)
val LightPlaceholderSand = Color(0xFFE7E1D3)

// Dark Mode colors (specifically tailored to complement dark teal grid mesh background #0A1E1C)
val DarkTextMint = Color(0xFFE5F3EC)          // Main text / Headings / Titles (Crisp Mint Cream)
val DarkFieldCream = Color(0xFF142C28)         // Fields / Inputs / Dropdowns (Dark Forest Teal)
val DarkTitleGray = DarkTextMint              // Title Gray
val DarkSubTextGray = DarkTextMint             // Subtitles / Secondary text (Memora Text Color)
val DarkAccentBrown = Color(0xFFE5C798)        // Accents / Section Headers / Notices (Warm Amber Gold)
val DarkCardStroke = Color(0xFF214A43)         // Borders / Dividers
val DarkCardBackground = Color(0xF40E2220)     // Card / Container Surfaces (Dark Teal Card)
val DarkButtonContainer = Color(0xFF216853)    // Primary Button Container (Deep Emerald)
val DarkPlaceholderSand = Color(0xFF1B3B37)   // Placeholder Photo Sand

val DarkGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkTextMint else LightDarkGreen

val FieldCream: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkFieldCream else LightFieldCream

val TitleGray: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkTitleGray else LightTitleGray

val SubTextGray: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkSubTextGray else LightSubTextGray

val AccentBrown: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkAccentBrown else LightAccentBrown

val CardStroke: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkCardStroke else LightCardStroke

val CardBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkCardBackground else Color.White

val PrimaryButtonContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkButtonContainer else LightDarkGreen

val PlaceholderSand: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkPlaceholderSand else LightPlaceholderSand

// Placeholder tile colors used for items/rooms without a photo yet — same warm neutral
// family already used inline in HomeScreen.
val PlaceholderEspresso = Color(0xFF5C4A32)
val PlaceholderCamel = Color(0xFFB7AC90)
val PlaceholderTan = Color(0xFFCFC2A8)
val PlaceholderWalnut = Color(0xFFA79E80)
val PlaceholderLightBrown = Color(0xFFD9CBB3)
val PlaceholderBlack = Color(0xFF3D3A32)
