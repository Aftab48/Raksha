package com.example.raksha.feature_trusted_contacts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raksha.core.datastore.AppPreferences
import com.example.raksha.core.datastore.StoredPermissionState
import com.example.raksha.core.permissions.PermissionUtils
import com.example.raksha.feature_trusted_contacts.data.local.entity.TrustedContactEntity
import com.example.raksha.feature_trusted_contacts.domain.model.TrustedContact
import com.example.raksha.feature_trusted_contacts.domain.usecase.AddTrustedContactUseCase
import com.example.raksha.feature_trusted_contacts.domain.usecase.DeleteTrustedContactUseCase
import com.example.raksha.feature_trusted_contacts.domain.usecase.ObserveTrustedContactsUseCase
import com.example.raksha.feature_trusted_contacts.domain.usecase.RefreshTrustedContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TrustedContactsStep {
    CONTACTS,
    PERMISSIONS,
    DONE
}

data class TrustedContactsOnboardingUiState(
    val isLoading: Boolean = true,
    val contacts: List<TrustedContactEntity> = emptyList(),
    val currentStep: TrustedContactsStep = TrustedContactsStep.CONTACTS,
    val storedPermissionState: StoredPermissionState = StoredPermissionState(),
    val message: String? = null,
    val errorMessage: String? = null,
    val onboardingCompleted: Boolean = false
)

@HiltViewModel
class TrustedContactsOnboardingViewModel @Inject constructor(
    private val observeTrustedContactsUseCase: ObserveTrustedContactsUseCase,
    private val refreshTrustedContactsUseCase: RefreshTrustedContactsUseCase,
    private val addTrustedContactUseCase: AddTrustedContactUseCase,
    private val deleteTrustedContactUseCase: DeleteTrustedContactUseCase,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrustedContactsOnboardingUiState())
    val uiState: StateFlow<TrustedContactsOnboardingUiState> = _uiState.asStateFlow()

    init {
        observeContacts()
        observeStoredPermissions()
        refreshTrustedContacts()
    }

    fun onAddContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null, message = null) }
            addTrustedContactUseCase(name, phoneNumber).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(message = "Trusted contact saved")
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { state ->
                        if (throwable.message?.startsWith("Saved locally") == true) {
                            state.copy(message = throwable.message)
                        } else {
                            state.copy(errorMessage = throwable.message ?: "Couldn't save trusted contact")
                        }
                    }
                }
            )
        }
    }

    fun onRemoveContact(contact: TrustedContactEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null, message = null) }
            deleteTrustedContactUseCase(contact.phoneNumber).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(message = "Trusted contact removed")
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { state ->
                        if (throwable.message?.startsWith("Removed locally") == true) {
                            state.copy(message = throwable.message)
                        } else {
                            state.copy(errorMessage = throwable.message ?: "Couldn't remove trusted contact")
                        }
                    }
                }
            )
        }
    }

    fun onContactsContinue() {
        if (_uiState.value.contacts.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add at least one trusted contact to continue") }
            return
        }

        _uiState.update {
            it.copy(
                currentStep = TrustedContactsStep.PERMISSIONS,
                errorMessage = null,
                message = null
            )
        }
    }

    fun onPermissionsBack() {
        _uiState.update {
            it.copy(currentStep = TrustedContactsStep.CONTACTS, errorMessage = null)
        }
    }

    fun onPermissionStateChanged(grants: Map<String, Boolean>) {
        val storedPermissionState = PermissionUtils.toStoredPermissionState(grants)
        _uiState.update {
            it.copy(storedPermissionState = storedPermissionState, errorMessage = null)
        }

        viewModelScope.launch {
            appPreferences.updatePermissionState(storedPermissionState)
        }
    }

    fun onPermissionsContinue(allPermissionsGranted: Boolean) {
        if (!allPermissionsGranted) {
            _uiState.update {
                it.copy(errorMessage = "Please grant all required permissions to continue")
            }
            return
        }

        _uiState.update {
            it.copy(
                currentStep = TrustedContactsStep.DONE,
                errorMessage = null,
                message = null
            )
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            appPreferences.setContactsSetup()
            _uiState.update {
                it.copy(onboardingCompleted = true)
            }
        }
    }

    private fun observeContacts() {
        viewModelScope.launch {
            observeTrustedContactsUseCase().collect { contacts ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        contacts = contacts.map { contact -> contact.toEntity() }
                    )
                }
            }
        }
    }

    private fun observeStoredPermissions() {
        viewModelScope.launch {
            appPreferences.storedPermissionState.collect { permissionState ->
                _uiState.update {
                    it.copy(storedPermissionState = permissionState)
                }
            }
        }
    }

    private fun refreshTrustedContacts() {
        viewModelScope.launch {
            refreshTrustedContactsUseCase().onFailure { throwable ->
                if (_uiState.value.contacts.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Couldn't load trusted contacts"
                        )
                    }
                }
            }
        }
    }

    private fun TrustedContact.toEntity(): TrustedContactEntity {
        return TrustedContactEntity(
            name = name,
            phoneNumber = phoneNumber,
            priority = priority
        )
    }
}
