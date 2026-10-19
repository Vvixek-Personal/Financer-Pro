package com.example.ui

import com.example.ui.theme.*
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Dedicated Language Settings Screen (Rule 5)
 * Allows searching and switching app language with full reactivity across all screens.
 */
@Composable
fun LanguageScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    var searchLanguage by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filteredLangs = remember(searchLanguage) {
        LanguageManager.supportedLanguages.filter {
            it.nativeName.contains(searchLanguage, ignoreCase = true) ||
            it.engName.contains(searchLanguage, ignoreCase = true) ||
            it.code.contains(searchLanguage, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Language Settings", onBack)

        Column(modifier = Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = searchLanguage,
                onValueChange = { searchLanguage = it },
                placeholder = { Text("Search language") },
                leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = SleekTextSecondary) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredLangs, key = { it.code }) { lang ->
                    val isSelected = selectedLanguage.equals(lang.nativeName, ignoreCase = true) || selectedLanguage.equals(lang.engName, ignoreCase = true)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) SleekPrimary.copy(alpha = 0.12f) else SleekSurface),
                        border = BorderStroke(1.dp, if (isSelected) SleekPrimary else SleekBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateLanguage(lang.nativeName)
                                Toast.makeText(context, "Language changed to ${lang.nativeName}", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(lang.flag, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(lang.nativeName, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                                    Text(lang.engName, style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                                }
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.updateLanguage(lang.nativeName) }
                            )
                        }
                    }
                }
            }
        }
    }
}
