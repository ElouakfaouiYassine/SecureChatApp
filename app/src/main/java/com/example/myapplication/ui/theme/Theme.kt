package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AppChatTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Purple200,
            secondary = Teal200,
            background = Color.Black,
            onBackground = Color.White
        )
    } else {
        lightColorScheme(
            primary = PrimaryColor,
            secondary = Teal200,
            background = BackgroundColor,
            onBackground = TextColor
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}