package com.raksha.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raksha.app.data.local.entity.TrustedContactEntity
import com.raksha.app.repository.TrustedContactRepository
import com.raksha.app.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,  // 0=Welcome, 1=Name/Phone, 2=Contacts, 3=Permissions, 4=Done
    val name: String = "",
    val phone: String = "",
    val contacts: List<TrustedContactEntity> = emptyList(),
    val nameError: String? = null,
    val phoneError: String? = null,
    val isComplete: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val contactRepository: TrustedContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, nameError = null)
    }

    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(phone = phone, phoneError = null)
    }

    fun nextStep() {
        val state = _uiState.value
        when (state.currentStep) {
            1 -> {
                if (state.name.isBlank()) {
                    _uiState.value = state.copy(nameError = "Name is required")
                    return
                }
                if (state.phone.isBlank() || state.phone.length < 10) {
                    _uiState.value = state.copy(phoneError = "Valid phone number required")
                    return
                }
                saveUser()
                _uiState.value = _uiState.value.copy(currentStep = 2)
            }
            2 -> {
                if (state.contacts.isEmpty()) return // enforce minimum 1 contact
                _uiState.value = state.copy(currentStep = 3)
            }
            else -> {
                _uiState.value = state.copy(currentStep = state.currentStep + 1)
            }
        }
    }

    fun previousStep() {
        val step = _uiState.value.currentStep
        if (step > 0) _uiState.value = _uiState.value.copy(currentStep = step - 1)
    }

    private fun saveUser() {
        viewModelScope.launch {
            val state = _uiState.value
            userRepository.saveUser(state.name.trim(), state.phone.trim())
        }
    }

    fun addContact(name: String, phone: String) {
        viewModelScope.launch {
            if (name.isBlank() || phone.isBlank()) return@launch
            val added = contactRepository.addContact(name.trim(), phone.trim())
            if (added) refreshContacts()
        }
    }

    fun removeContact(contact: TrustedContactEntity) {
        viewModelScope.launch {
            contactRepository.deleteContact(contact)
            refreshContacts()
        }
    }

    private fun refreshContacts() {
        viewModelScope.launch {
            val contacts = contactRepository.getContactsOnce()
            _uiState.value = _uiState.value.copy(contacts = contacts)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userRepository.markOnboardingComplete()
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }
}
