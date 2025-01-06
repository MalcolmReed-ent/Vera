package com.logan.vera.ui.theme

import androidx.compose.ui.graphics.Color

enum class ReaderTheme(
    val background: Color,
    val text: Color,
    val displayName: String
) {
    LIGHT(
        background = Color(0xFFFFFFFF),
        text = Color(0xFF000000),
        displayName = "Light"
    ),
    DARK(
        background = Color(0xFF121212),
        text = Color(0xFFE1E1E1),
        displayName = "Dark"
    ),
    SEPIA(
        background = Color(0xFFF8F1E3),
        text = Color(0xFF5B4636),
        displayName = "Sepia"
    )
}
