package com.raksha.app.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raksha.app.ui.component.ContactCard
import com.raksha.app.ui.theme.*
import com.raksha.app.viewmodel.SettingsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val sosEvents by viewModel.sosEvents.collectAsState()
    var pendingDeleteContact by remember { mutableStateOf<com.raksha.app.data.local.entity.TrustedContactEntity?>(null) }
    var keywordInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.helpKeywordError, uiState.helpKeywordMessage) {
        if (uiState.helpKeywordError != null || uiState.helpKeywordMessage != null) {
            kotlinx.coroutines.delay(3500L)
            viewModel.clearHelpKeywordMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Top bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .background(ColorSurface.copy(alpha = 0.92f), RakshaShapes.large)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = ColorTextPrimary)
                    }
                    Text("Settings", style = RakshaTypography.headlineLarge)
                }
            }

            // User info
            uiState.user?.let { user ->
                item {
                    SectionHeader("Account")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(ColorSurface, RakshaShapes.medium)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(ColorPrimarySubtle, RadiusFull),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.name.take(2).uppercase(),
                                style = RakshaTypography.headlineMedium.copy(color = ColorPrimary)
                            )
                        }
                        Column {
                            Text(user.name, style = RakshaTypography.bodyLarge)
                            Text(user.phone, style = RakshaTypography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Trusted contacts
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeaderInline("Trusted Contacts (${contacts.size}/5)")
                    if (uiState.canAddMoreContacts) {
                        IconButton(onClick = viewModel::showAddContactDialog) {
                            Icon(Icons.Filled.Add, contentDescription = "Add contact", tint = ColorPrimary)
                        }
                    }
                }
            }
            uiState.contactSyncError?.let { error ->
                item {
                    Text(
                        text = error,
                        style = RakshaTypography.bodyMedium.copy(color = ColorDanger),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            uiState.contactSyncMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = RakshaTypography.bodyMedium.copy(color = ColorSafe),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            if (contacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(ColorSurface, RakshaShapes.medium)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No contacts yet. Add at least 1 to enable Shield.",
                            style = RakshaTypography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(contacts) { contact ->
                    ContactCard(
                        contact = contact,
                        onDeleteRequest = { pendingDeleteContact = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        deleteEnabled = uiState.deletingContactId != contact.id
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            item {
                SectionHeaderInline("Help Keywords (${uiState.helpKeywords.size}/15)")
            }

            item {
                Text(
                    text = "Shield matches these keywords against local audio model classes and auto-triggers SOS when matched.",
                    style = RakshaTypography.bodySmall.copy(color = ColorTextSecondary),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Add keyword (e.g. help, bachao)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            cursorColor = ColorPrimary
                        ),
                        shape = RakshaShapes.medium
                    )
                    Button(
                        onClick = {
                            viewModel.addHelpKeyword(keywordInput)
                            keywordInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                        enabled = keywordInput.isNotBlank()
                    ) {
                        Text("Add", color = ColorTextInverse)
                    }
                }
            }

            uiState.helpKeywordError?.let { error ->
                item {
                    Text(
                        text = error,
                        style = RakshaTypography.bodyMedium.copy(color = ColorDanger),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
            }

            uiState.helpKeywordMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = RakshaTypography.bodyMedium.copy(color = ColorSafe),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
            }

            if (uiState.helpKeywords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(ColorSurface, RakshaShapes.medium)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "No keywords added. Add words like help, bachao, save me.",
                            style = RakshaTypography.bodyMedium.copy(color = ColorTextSecondary)
                        )
                    }
                }
            } else {
                items(uiState.helpKeywords) { keyword ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .border(1.dp, ColorBorder, RakshaShapes.medium)
                            .background(ColorSurface, RakshaShapes.medium)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = keyword,
                            style = RakshaTypography.bodyMedium,
                            color = ColorTextPrimary
                        )
                        IconButton(onClick = { viewModel.removeHelpKeyword(keyword) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Remove keyword",
                                tint = ColorDanger
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            // SOS event history
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeaderInline("SOS History")
                    if (sosEvents.isNotEmpty()) {
                        TextButton(onClick = viewModel::showClearHistoryDialog) {
                            Text("Clear", color = ColorDanger)
                        }
                    }
                }
            }

            if (sosEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(ColorSurface, RakshaShapes.medium)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No past alerts.", style = RakshaTypography.bodyMedium)
                    }
                }
            } else {
                items(sosEvents) { event ->
                    SosEventItem(
                        timestamp = event.timestamp,
                        triggerType = event.triggerType,
                        confidenceScore = event.confidenceScore,
                        status = event.status,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // Add contact dialog
    if (uiState.showAddContactDialog) {
        AddContactDialog(
            onAdd = viewModel::addContact,
            onDismiss = viewModel::dismissAddContactDialog
        )
    }

    // Clear history dialog
    if (uiState.showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearHistoryDialog,
            containerColor = ColorSurfaceElevated,
            title = { Text("Clear History", style = RakshaTypography.headlineMedium) },
            text = { Text("This will permanently delete all past SOS events.", style = RakshaTypography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = viewModel::clearHistory,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)
                ) { Text("Clear All", color = ColorTextPrimary) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearHistoryDialog) {
                    Text("Cancel", color = ColorTextSecondary)
                }
            }
        )
    }

    pendingDeleteContact?.let { contact ->
        AlertDialog(
            onDismissRequest = { pendingDeleteContact = null },
            containerColor = ColorSurfaceElevated,
            title = { Text("Remove Contact", style = RakshaTypography.headlineMedium) },
            text = {
                Text(
                    "Delete ${contact.name} from trusted contacts?",
                    style = RakshaTypography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteContact(contact)
                        pendingDeleteContact = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)
                ) {
                    Text("Delete", color = ColorTextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteContact = null }) {
                    Text("Cancel", color = ColorTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SosEventItem(
    timestamp: String,
    triggerType: String,
    confidenceScore: Double,
    status: String,
    modifier: Modifier = Modifier
) {
    val formattedTime = remember(timestamp) {
        try {
            val instant = Instant.parse(timestamp)
            DateTimeFormatter
                .ofPattern("dd MMM yyyy, HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) {
            timestamp
        }
    }

    Row(
        modifier = modifier
            .background(ColorSurface, RakshaShapes.medium)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formattedTime, style = RakshaTextStyle.mono)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (triggerType == "auto") "Auto-detected" else "Manual",
                    style = RakshaTypography.labelMedium
                )
                if (triggerType == "auto" && confidenceScore > 0) {
                    Text(
                        text = "${(confidenceScore * 100).toInt()}% confidence",
                        style = RakshaTypography.labelMedium.copy(color = ColorTextSecondary)
                    )
                }
            }
        }
        Surface(
            color = if (status == "active") ColorDangerSubtle else ColorSafeSubtle,
            shape = RadiusFull
        ) {
            Text(
                text = status.replaceFirstChar { it.uppercase() },
                style = RakshaTypography.labelMedium.copy(
                    color = if (status == "active") ColorDanger else ColorSafe
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = RakshaTypography.labelLarge.copy(color = ColorTextSecondary),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SectionHeaderInline(title: String) {
    Text(
        text = title,
        style = RakshaTypography.labelLarge.copy(color = ColorTextSecondary),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun AddContactDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurfaceElevated,
        title = { Text("Add Trusted Contact", style = RakshaTypography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = ColorBorder,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary,
                        cursorColor = ColorPrimary
                    ),
                    shape = RakshaShapes.medium
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = ColorBorder,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary,
                        cursorColor = ColorPrimary
                    ),
                    shape = RakshaShapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onAdd(name, phone) },
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
            ) { Text("Add", color = ColorTextInverse) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ColorTextSecondary) }
        }
    )
}
