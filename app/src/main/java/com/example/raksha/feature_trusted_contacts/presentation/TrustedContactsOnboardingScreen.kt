package com.example.raksha.feature_trusted_contacts.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.raksha.core.permissions.PermissionUtils
import com.example.raksha.feature_trusted_contacts.data.local.entity.TrustedContactEntity
import com.example.raksha.ui.theme.ColorBorder
import com.example.raksha.ui.theme.ColorDanger
import com.example.raksha.ui.theme.ColorPrimary
import com.example.raksha.ui.theme.ColorSurface
import com.example.raksha.ui.theme.ColorSurfaceElevated
import com.example.raksha.ui.theme.ColorTextInverse
import com.example.raksha.ui.theme.ColorTextPrimary
import com.example.raksha.ui.theme.ColorTextSecondary
import com.example.raksha.ui.theme.RadiusFull
import com.example.raksha.ui.theme.RakshaShapes
import com.example.raksha.ui.theme.RakshaTypography
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@Composable
fun TrustedContactsOnboardingRoute(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    viewModel: TrustedContactsOnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.onboardingCompleted) {
        if (uiState.onboardingCompleted) {
            onFinish()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ColorSurface
    ) {
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = ColorPrimary)
                Spacer(Modifier.height(16.dp))
                Text("Loading your safety setup...", style = RakshaTypography.bodyMedium)
            }
        } else {
            when (uiState.currentStep) {
                TrustedContactsStep.CONTACTS -> ContactsStep(
                    contacts = uiState.contacts,
                    onAddContact = viewModel::onAddContact,
                    onRemoveContact = viewModel::onRemoveContact,
                    onNext = viewModel::onContactsContinue,
                    onBack = onBack,
                    helperMessage = uiState.message,
                    errorMessage = uiState.errorMessage
                )

                TrustedContactsStep.PERMISSIONS -> PermissionsStep(
                    onNext = viewModel::onPermissionsContinue,
                    onBack = viewModel::onPermissionsBack,
                    onPermissionStateChanged = viewModel::onPermissionStateChanged,
                    errorMessage = uiState.errorMessage
                )

                TrustedContactsStep.DONE -> DoneStep(
                    onFinish = viewModel::finishOnboarding
                )
            }
        }
    }
}

@Composable
private fun ContactsStep(
    contacts: List<TrustedContactEntity>,
    onAddContact: (String, String) -> Unit,
    onRemoveContact: (TrustedContactEntity) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    helperMessage: String?,
    errorMessage: String?
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Trusted Contacts", style = RakshaTypography.headlineLarge)
        Text(
            "Add up to 5 people who will be alerted in an emergency. Minimum 1 required.",
            style = RakshaTypography.bodyMedium
        )

        contacts.forEach { contact ->
            ContactCard(contact = contact, onDelete = onRemoveContact)
        }

        if (contacts.size < 5) {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RakshaShapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Contact")
            }
        }

        if (!helperMessage.isNullOrBlank()) {
            Text(helperMessage, style = RakshaTypography.bodyMedium)
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage, color = ColorDanger, style = RakshaTypography.bodyMedium)
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RadiusFull,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextSecondary),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Text("Back")
            }

            Button(
                onClick = onNext,
                enabled = contacts.isNotEmpty(),
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp),
                shape = RadiusFull,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
            ) {
                Text("Continue", color = ColorTextInverse, style = RakshaTypography.bodyLarge)
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            onAdd = { name, phone ->
                onAddContact(name, phone)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionsStep(
    onNext: (Boolean) -> Unit,
    onBack: () -> Unit,
    onPermissionStateChanged: (Map<String, Boolean>) -> Unit,
    errorMessage: String?
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionUtils.ONBOARDING_PERMISSIONS
    )

    LaunchedEffect(permissionsState.permissions.map { it.status.isGranted }) {
        onPermissionStateChanged(
            permissionsState.permissions.associate { state ->
                state.permission to state.status.isGranted
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Permissions Needed", style = RakshaTypography.headlineLarge)
        Text(
            "Raksha needs these permissions to protect you. None of your data leaves your device.",
            style = RakshaTypography.bodyMedium
        )

        PermissionRow("Microphone", "Detects distress sounds when Shield is active")
        PermissionRow("Location", "Sends your exact location during SOS alerts")
        PermissionRow("SMS", "Alerts your trusted contacts with your location")
        PermissionRow("Phone", "Auto-dials 112 when an emergency is detected")

        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage, color = ColorDanger, style = RakshaTypography.bodyMedium)
        }

        Spacer(Modifier.weight(1f))

        if (!permissionsState.allPermissionsGranted) {
            Button(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RadiusFull,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
            ) {
                Text("Grant Permissions", color = ColorTextInverse, style = RakshaTypography.bodyLarge)
            }
        } else {
            Button(
                onClick = { onNext(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RadiusFull,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
            ) {
                Text("Continue", color = ColorTextInverse, style = RakshaTypography.bodyLarge)
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back", color = ColorTextSecondary)
        }
    }
}

@Composable
private fun PermissionRow(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurfaceElevated, RakshaShapes.medium)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(title, style = RakshaTypography.bodyLarge)
            Text(description, style = RakshaTypography.bodyMedium)
        }
    }
}

@Composable
private fun DoneStep(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = ColorPrimary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(32.dp))
        Text("You're protected", style = RakshaTypography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Use the Shield toggle on the home screen to activate audio monitoring whenever you feel unsafe. You can always add more contacts in Settings.",
            style = RakshaTypography.bodyLarge.copy(color = ColorTextSecondary),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(64.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RadiusFull,
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
        ) {
            Text("Open Raksha", color = ColorTextInverse, style = RakshaTypography.bodyLarge)
        }
    }
}

@Composable
private fun ContactCard(
    contact: TrustedContactEntity,
    onDelete: (TrustedContactEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurfaceElevated, RakshaShapes.medium)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .size(40.dp)
                .background(ColorPrimary.copy(alpha = 0.12f), CircleShape),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = null,
                tint = ColorPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, style = RakshaTypography.bodyLarge)
            Text(contact.phoneNumber, style = RakshaTypography.bodyMedium)
        }

        Text(
            text = "#${contact.priority}",
            style = RakshaTypography.labelMedium
        )

        IconButton(onClick = { onDelete(contact) }) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Remove contact",
                tint = ColorDanger
            )
        }
    }
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
                    colors = rakshaTextFieldColors(),
                    shape = RakshaShapes.medium,
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = rakshaTextFieldColors(),
                    shape = RakshaShapes.medium,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onAdd(name, phone) },
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
            ) {
                Text("Add", color = ColorTextInverse)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ColorTextSecondary) }
        }
    )
}

@Composable
private fun rakshaTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ColorPrimary,
    unfocusedBorderColor = ColorBorder,
    focusedLabelColor = ColorPrimary,
    unfocusedLabelColor = ColorTextSecondary,
    cursorColor = ColorPrimary,
    focusedTextColor = ColorTextPrimary,
    unfocusedTextColor = ColorTextPrimary
)
