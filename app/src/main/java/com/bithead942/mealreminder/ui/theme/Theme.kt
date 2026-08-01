package com.bithead942.mealreminder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cyan = Color(0xFF20CFE3)
val CyanDark = Color(0xFF12B6C9)
val InkBlue = Color(0xFF2B4A57)
val TextPrimary = Color(0xFF25353F)
val TextMuted = Color(0xFFA9B4BB)
val DisabledPill = Color(0xFFD7DDE0)
val Divider = Color(0xFFEDF0F1)
val TrackGrey = Color(0xFFDDE2E4)
val BadgeGrey = Color(0xFF9AAAB2)

private val LightColors = lightColorScheme(
    primary = Cyan,
    onPrimary = Color.White,
    secondary = CyanDark,
    background = Color.White,
    surface = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val DarkColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Color.Black,
    secondary = CyanDark
)

@Composable
fun MealReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
