package com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text

/**
 * Form for adding a new repository
 */
@Composable
fun AddRepositoryForm(
    viewModel: ExtensionsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TextField dla nazwy (opcjonalne)
        TvTextField(
            value = name,
            onValueChange = { name = it },
            label = "Nazwa repozytorium (opcjonalne)",
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        // TextField dla URL (wymagane)
        TvTextField(
            value = url,
            onValueChange = { url = it },
            label = "URL repozytorium *",
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Uri
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Error message
        error?.let {
            Text(
                text = it,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Button Dodaj
        Button(
            onClick = {
                if (url.isBlank()) {
                    error = "Podaj URL repozytorium"
                    return@Button
                }
                
                isLoading = true
                error = null
                
                // Pass null as name if empty, so it will be auto-detected
                val repoName = name.ifBlank { null }
                
                viewModel.addRepository(repoName, url.trim()) { result ->
                    isLoading = false
                    android.util.Log.d("AddRepositoryForm", "addRepository callback called, success=${result.isSuccess}")
                    result.fold(
                        onSuccess = { addedRepo ->
                            // Successfully added, go back immediately
                            // The list will update automatically via ViewModel
                            android.util.Log.d("AddRepositoryForm", "Calling onBack() after successful add")
                            onBack()
                        },
                        onFailure = { e ->
                            android.util.Log.e("AddRepositoryForm", "Failed to add repository: ${e.message}")
                            error = when {
                                e.message?.contains("Invalid repository URL") == true -> 
                                    "Nieprawidłowy URL repozytorium"
                                e.message?.contains("No repository found") == true -> 
                                    "Nie znaleziono repozytorium pod tym adresem"
                                e.message?.contains("already exists") == true -> 
                                    "To repozytorium już istnieje"
                                else -> "Błąd: ${e.message ?: "Nieznany błąd"}"
                            }
                        }
                    )
                }
            },
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Dodawanie..." else "Dodaj repozytorium")
        }
    }
}

/**
 * Simple TextField wrapper for TV with proper styling and single line
 */
@Composable
private fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            colors = SurfaceDefaults.colors(
                containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = ShapeDefaults.Small
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
        }
    }
}
