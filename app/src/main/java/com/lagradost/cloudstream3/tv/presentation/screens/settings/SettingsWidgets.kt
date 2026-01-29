package com.lagradost.cloudstream3.tv.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text

/**
 * Leaf widgets for the Settings screen.
 * These are the actual interactive elements shown when navigating to a leaf node (node with content).
 */

@Composable
fun SwitchSettingWidget(
    title: String,
    description: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEnabled by rememberSaveable { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Surface(
            onClick = { isEnabled = !isEnabled },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
            shape = ClickableSurfaceDefaults.shape(
                shape = RoundedCornerShape(8.dp)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEnabled) "Włączone" else "Wyłączone",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Naciśnij aby zmienić",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = null // Handled by Surface onClick
                )
            }
        }
    }
}

// ============================================================================
// LEVEL 4 WIDGETS - TEXT FIELD
// ============================================================================

@Composable
fun TextFieldSettingWidget(
    title: String,
    description: String,
    initialValue: String,
    onValueSaved: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by rememberSaveable { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        BasicTextField(
            value = textValue,
            onValueChange = { textValue = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onValueSaved(textValue)
                    onBack()
                }
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (textValue.isEmpty()) {
                        Text(
                            text = "Wprowadź wartość...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Naciśnij Enter aby zapisać",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// PLACEHOLDER WIDGETS FOR LEVEL 4
// ============================================================================

@Composable
fun LanguageSelector(language: String, onBack: () -> Unit) {
    PlaceholderWidget(
        title = "Język: $language",
        description = "Wybrano język: $language",
        onBack = onBack
    )
}

@Composable
fun DeviceNameEditor(onBack: () -> Unit) {
    TextFieldSettingWidget(
        title = "Nazwa urządzenia",
        description = "Wprowadź nazwę dla tego urządzenia",
        initialValue = "Google TV",
        onValueSaved = { /* Save device name */ },
        onBack = onBack
    )
}

@Composable
fun DeviceIdViewer(onBack: () -> Unit) {
    PlaceholderWidget(
        title = "ID urządzenia",
        description = "ABC123-DEF456-GHI789",
        onBack = onBack
    )
}

@Composable
fun QualitySelector(quality: String, onBack: () -> Unit) {
    PlaceholderWidget(
        title = "Jakość: $quality",
        description = "Wybrana jakość streamingu: $quality",
        onBack = onBack
    )
}

@Composable
fun SubtitleSizeSelector(onBack: () -> Unit) {
    var selectedSize by rememberSaveable { mutableIntStateOf(1) }
    val sizes = listOf("Mały", "Średni", "Duży", "Bardzo duży")

    ListSelectorWidget(
        title = "Rozmiar napisów",
        options = sizes,
        selectedIndex = selectedSize,
        onSelect = { selectedSize = it },
        onBack = onBack
    )
}

@Composable
fun SubtitleColorSelector(onBack: () -> Unit) {
    var selectedColor by rememberSaveable { mutableIntStateOf(0) }
    val colors = listOf("Biały", "Żółty", "Zielony", "Niebieski", "Czerwony")

    ListSelectorWidget(
        title = "Kolor napisów",
        options = colors,
        selectedIndex = selectedColor,
        onSelect = { selectedColor = it },
        onBack = onBack
    )
}

@Composable
fun SeekDurationSelector(onBack: () -> Unit) {
    var selectedDuration by rememberSaveable { mutableIntStateOf(1) }
    val durations = listOf("5 sekund", "10 sekund", "15 sekund", "30 sekund", "60 sekund")

    ListSelectorWidget(
        title = "Czas przewijania",
        options = durations,
        selectedIndex = selectedDuration,
        onSelect = { selectedDuration = it },
        onBack = onBack
    )
}

@Composable
fun DecoderSelector(onBack: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val options = listOf("Automatyczny", "Sprzętowy", "Programowy")

    ListSelectorWidget(
        title = "Dekoder wideo",
        options = options,
        selectedIndex = selected,
        onSelect = { selected = it },
        onBack = onBack
    )
}

@Composable
fun BufferSizeSelector(onBack: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(1) }
    val options = listOf("Mały (16MB)", "Średni (32MB)", "Duży (64MB)", "Bardzo duży (128MB)")

    ListSelectorWidget(
        title = "Rozmiar bufora",
        options = options,
        selectedIndex = selected,
        onSelect = { selected = it },
        onBack = onBack
    )
}

@Composable
fun ThemeSelector(theme: String, onBack: () -> Unit) {
    PlaceholderWidget(
        title = "Motyw: $theme",
        description = "Wybrany motyw kolorystyczny: $theme",
        onBack = onBack
    )
}

@Composable
fun LayoutSelector(layout: String, onBack: () -> Unit) {
    PlaceholderWidget(
        title = "Układ: $layout",
        description = "Wybrany układ interfejsu: $layout",
        onBack = onBack
    )
}

@Composable
fun InfoDisplay(title: String, info: String, onBack: () -> Unit) {
    PlaceholderWidget(
        title = title,
        description = info,
        onBack = onBack
    )
}

@Composable
fun TimeoutEditor(onBack: () -> Unit) {
    TextFieldSettingWidget(
        title = "Limit czasu (sekundy)",
        description = "Maksymalny czas oczekiwania na odpowiedź źródła",
        initialValue = "30",
        onValueSaved = { /* Save timeout */ },
        onBack = onBack
    )
}

@Composable
fun ProfileNameEditor(onBack: () -> Unit) {
    TextFieldSettingWidget(
        title = "Nazwa profilu",
        description = "Wprowadź swoją nazwę użytkownika",
        initialValue = "",
        onValueSaved = { /* Save profile name */ },
        onBack = onBack
    )
}

@Composable
fun AvatarSelector(onBack: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val options = listOf("Domyślny", "Niebieski", "Zielony", "Czerwony", "Żółty", "Fioletowy")

    ListSelectorWidget(
        title = "Wybierz awatar",
        options = options,
        selectedIndex = selected,
        onSelect = { selected = it },
        onBack = onBack
    )
}

@Composable
fun ActionButton(title: String, description: String, onBack: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Surface(
            onClick = { /* Execute action */ },
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                ,
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            ),
            shape = ClickableSurfaceDefaults.shape(
                shape = RoundedCornerShape(8.dp)
            )
        ) {
            Text(
                text = "Wykonaj",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun ExtensionDetails(name: String, version: String, onBack: () -> Unit) {
    SwitchSettingWidget(
        title = name,
        description = "Wersja: $version - Włącz lub wyłącz to rozszerzenie",
        onBack = onBack
    )
}

@Composable
fun RepoUrlEditor(onBack: () -> Unit) {
    TextFieldSettingWidget(
        title = "URL repozytorium",
        description = "Wprowadź adres URL repozytorium rozszerzeń",
        initialValue = "",
        onValueSaved = { /* Save repo URL */ },
        onBack = onBack
    )
}

@Composable
fun RepoList(onBack: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val repos = listOf(
        "Oficjalne repozytorium",
        "Community Extensions",
        "Beta Extensions",
        "Custom Repo 1"
    )

    ListSelectorWidget(
        title = "Repozytoria",
        options = repos,
        selectedIndex = selected,
        onSelect = { selected = it },
        onBack = onBack
    )
}

@Composable
fun ExtensionBrowser(category: String, onBack: () -> Unit) {
    PlaceholderWidget(
        title = "Przeglądaj: $category",
        description = "Tutaj pojawi się lista dostępnych rozszerzeń z kategorii: $category",
        onBack = onBack
    )
}

// ============================================================================
// REUSABLE WIDGET COMPONENTS
// ============================================================================

@Composable
fun PlaceholderWidget(
    title: String,
    description: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            onClick = { /* Handle click */ },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
            shape = ClickableSurfaceDefaults.shape(
                shape = RoundedCornerShape(8.dp)
            )
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun ListSelectorWidget(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.focusGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(options) { index, option ->
                var isFocused by remember { mutableStateOf(false) }
                val focusRequester = remember { FocusRequester() }
                val isSelected = index == selectedIndex
                val isFirstItem = index == 0

                Surface(
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    shape = ClickableSurfaceDefaults.shape(
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (isSelected) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Auto-focus selected item or first item
                if (index == selectedIndex || (selectedIndex < 0 && index == 0)) {
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }
            }
        }
    }
}
