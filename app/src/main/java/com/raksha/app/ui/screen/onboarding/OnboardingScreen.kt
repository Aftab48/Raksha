package com.raksha.app.ui.screen.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.raksha.app.ui.component.ContactCard
import com.raksha.app.ui.theme.*
import com.raksha.app.utils.PermissionUtils
import com.raksha.app.viewmodel.OnboardingViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
            },
            label = "onboarding_step"
        ) { step ->
            when (step) {
                0 -> WelcomeStep(onNext = viewModel::nextStep)
                1 -> NamePhoneStep(
                    name = state.name,
                    phone = state.phone,
                    nameError = state.nameError,
                    phoneError = state.phoneError,
                    onNameChange = viewModel::updateName,
                    onPhoneChange = viewModel::updatePhone,
                    onNext = viewModel::nextStep
                )
                2 -> ContactsStep(
                    contacts = state.contacts,
                    syncMessage = state.contactSyncMessage,
                    syncError = state.contactSyncError,
                    onAddContact = viewModel::addContact,
                    onRemoveContact = viewModel::removeContact,
                    onNext = viewModel::nextStep,
                    onBack = viewModel::previousStep
                )
                3 -> PermissionsStep(
                    onNext = viewModel::nextStep,
                    onBack = viewModel::previousStep
                )
                4 -> DoneStep(onFinish = viewModel::completeOnboarding)
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
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
        Text(
            text = "Raksha",
            style = RakshaTypography.displayLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Your silent guardian.\nAlways watching. Always ready.",
            style = RakshaTypography.bodyLarge.copy(color = ColorTextSecondary),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(64.dp))
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RadiusFull,
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
        ) {
            Text("Get Started", color = ColorTextInverse, style = RakshaTypography.bodyLarge)
        }
    }
}

@Composable
private fun NamePhoneStep(
    name: String,
    phone: String,
    nameError: String?,
    phoneError: String?,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Who are you?", style = RakshaTypography.headlineLarge)
        Text(
            "Your name and phone are stored only on your device.",
            style = RakshaTypography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your name") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            colors = rakshaTextFieldColors(),
            shape = RakshaShapes.medium
        )
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Your phone number") },
            isError = phoneError != null,
            supportingText = phoneError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = rakshaTextFieldColors(),
            shape = RakshaShapes.medium
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RadiusFull,
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
        ) {
            Text("Continue", color = ColorTextInverse, style = RakshaTypography.bodyLarge)
        }
    }
}

@Composable
private fun ContactsStep(
    contacts: List<com.raksha.app.data.local.entity.TrustedContactEntity>,
    syncMessage: String?,
    syncError: String?,
    onAddContact: (String, String) -> Unit,
    onRemoveContact: (com.raksha.app.data.local.entity.TrustedContactEntity) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
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
        syncError?.let {
            Text(
                text = it,
                style = RakshaTypography.bodyMedium.copy(color = ColorDanger)
            )
        }
        syncMessage?.let {
            Text(
                text = it,
                style = RakshaTypography.bodyMedium.copy(color = ColorSafe)
            )
        }

        contacts.forEach { contact ->
            ContactCard(contact = contact, onDelete = onRemoveContact)
        }

        if (contacts.size < 5) {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RakshaShapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorder)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Contact")
            }
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RadiusFull,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorder)
            ) { Text("Back") }

            Button(
                onClick = onNext,
                enabled = contacts.isNotEmpty(),
                modifier = Modifier.weight(2f).height(52.dp),
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
private fun PermissionsStep(onNext: () -> Unit, onBack: () -> Unit) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionUtils.ONBOARDING_PERMISSIONS.toList()
    )

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

        Spacer(Modifier.weight(1f))

        if (!permissionsState.allPermissionsGranted) {
            Button(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RadiusFull,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
            ) {
                Text("Grant Permissions", color = ColorTextInverse, style = RakshaTypography.bodyLarge)
            }
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
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
            .background(ColorSurface, RakshaShapes.medium)
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
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RadiusFull,
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
        ) {
            Text("Open Raksha", color = ColorTextInverse, style = RakshaTypography.bodyLarge)
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
                    shape = RakshaShapes.medium
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = rakshaTextFieldColors(),
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
