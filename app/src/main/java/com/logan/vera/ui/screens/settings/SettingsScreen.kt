package com.logan.vera.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.logan.vera.ui.theme.Fonts
import com.logan.vera.ui.theme.ReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFontPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Theme
            item {
                SettingsCard(title = "App Theme") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Dark Mode")
                        }
                        Switch(
                            checked = uiState.isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() }
                        )
                    }
                }
            }

            // Reader Settings
            item {
                SettingsCard(title = "Reader Settings") {
                    // Font Size
                    Column {
                        Text(
                            text = "Font Size",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("A", style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = uiState.fontSize,
                                onValueChange = { viewModel.updateFontSize(it) },
                                valueRange = 12f..24f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("A", style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Font Family
                    Column {
                        Text(
                            text = "Font Family",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showFontPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Change Font",
                                fontFamily = uiState.selectedFont
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Reader Theme
                    Column {
                        Text(
                            text = "Reader Theme",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ReaderTheme.values().forEach { theme ->
                                ThemeButton(
                                    theme = theme,
                                    isSelected = uiState.selectedTheme == theme,
                                    onClick = { viewModel.updateTheme(theme) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFontPicker) {
        FontPickerDialog(
            currentFont = uiState.selectedFont,
            onFontSelected = {
                viewModel.updateFont(it)
                showFontPicker = false
            },
            onDismiss = { showFontPicker = false }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ThemeButton(
    theme: ReaderTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = theme.background,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) theme.text else Color.Transparent
        ),
        modifier = Modifier.size(48.dp)
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = theme.text,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun FontPickerDialog(
    currentFont: FontFamily,
    onFontSelected: (FontFamily) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Font") },
        text = {
            LazyColumn {
                items(
                    listOf(
                        Fonts.Arimo to "Arimo",
                        Fonts.Garamond to "Garamond",
                        Fonts.Lora to "Lora",
                        Fonts.Merriweather to "Merriweather",
                        Fonts.Montserrat to "Montserrat",
                        Fonts.Mulish to "Mulish",
                        Fonts.NotoSans to "NotoSans",
                        Fonts.NotoSerif to "Noto Serif",
                        Fonts.Nunito to "Nunito",
                        Fonts.OpenSans to "Open Sans",
                        Fonts.Raleway to "Raleway",
                        Fonts.Roboto to "Roboto",
                        Fonts.RobotoSerif to "Roboto Serif",
                        Fonts.RobotoSlab to "Roboto Slab"
                    )
                ) { (font, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFontSelected(font) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFont == font,
                            onClick = { onFontSelected(font) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = name,
                            fontFamily = font
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
