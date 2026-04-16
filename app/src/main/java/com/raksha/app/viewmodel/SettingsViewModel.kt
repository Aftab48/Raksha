package com.raksha.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raksha.app.data.local.entity.SosEventEntity
import com.raksha.app.data.local.entity.TrustedContactEntity
import com.raksha.app.data.local.entity.UserEntity
import com.raksha.app.repository.SosRepository
import com.raksha.app.repository.TrustedContactRepository
import com.raksha.app.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: UserEntity? = null,
    val showClearHistoryDialog: Boolean = false,
    val showAddContactDialog: Boolean = false,
    val canAddMoreContacts: Boolean = true,
    val contactSyncMessage: String? = null,
    val contactSyncError: String? = null,
    val deletingContactId: Int? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val contactRepository: TrustedContactRepository,
    private val sosRepository: SosRepository
) : ViewModel() {

    val contacts: StateFlow<List<TrustedContactEntity>> = contactRepository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sosEvents: StateFlow<List<SosEventEntity>> = sosRepository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadUser()
        observeContactCount()
        syncContactsFromRemote()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val user = userRepository.getUserOnce()
            _uiState.value = _uiState.value.copy(user = user)
        }
    }

    private fun observeContactCount() {
        viewModelScope.launch {
            contactRepository.contacts.collect { list ->
                _uiState.value = _uiState.value.copy(canAddMoreContacts = list.size < 5)
            }
        }
    }

    fun addContact(name: String, phone: String) {
        viewModelScope.launch {
            contactRepository.addContact(name.trim(), phone.trim()).fold(
                onSuccess = { added ->
                    _uiState.value = if (!added) {
                        _uiState.value.copy(
                            contactSyncError = "You can add up to 5 trusted contacts only",
                            contactSyncMessage = null
                        )
                    } else {
                        _uiState.value.copy(
                            showAddContactDialog = false,
                            contactSyncMessage = "Trusted contact saved",
                            contactSyncError = null
                        )
                    }
                },
                onFailure = { throwable ->
                    val message = throwable.message ?: "Couldn't save trusted contact"
                    _uiState.value = if (message.startsWith("Saved locally")) {
                        _uiState.value.copy(
                            showAddContactDialog = false,
                            contactSyncMessage = message,
                            contactSyncError = null
                        )
                    } else {
                        _uiState.value.copy(
                            contactSyncError = message,
                            contactSyncMessage = null
                        )
                    }
                }
            )
        }
    }

    fun deleteContact(contact: TrustedContactEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingContactId = contact.id)
            contactRepository.deleteContact(contact).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        contactSyncMessage = "Trusted contact removed",
                        contactSyncError = null,
                        deletingContactId = null
                    )
                },
                onFailure = { throwable ->
                    val message = throwable.message ?: "Couldn't remove trusted contact"
                    _uiState.value = if (message.startsWith("Removed locally")) {
                        _uiState.value.copy(
                            contactSyncMessage = message,
                            contactSyncError = null,
                            deletingContactId = null
                        )
                    } else {
                        _uiState.value.copy(
                            contactSyncError = message,
                            contactSyncMessage = null,
                            deletingContactId = null
                        )
                    }
                }
            )
        }
    }

    fun showAddContactDialog() {
        _uiState.value = _uiState.value.copy(showAddContactDialog = true)
    }

    fun dismissAddContactDialog() {
        _uiState.value = _uiState.value.copy(showAddContactDialog = false, contactSyncError = null)
    }

    fun showClearHistoryDialog() {
        _uiState.value = _uiState.value.copy(showClearHistoryDialog = true)
    }

    fun dismissClearHistoryDialog() {
        _uiState.value = _uiState.value.copy(showClearHistoryDialog = false)
    }

    fun clearHistory() {
        viewModelScope.launch {
            sosRepository.clearHistory()
            _uiState.value = _uiState.value.copy(showClearHistoryDialog = false)
        }
    }

    private fun syncContactsFromRemote() {
        viewModelScope.launch {
            contactRepository.refreshContactsFromRemote().onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    contactSyncError = throwable.message ?: "Couldn't sync trusted contacts"
                )
            }
        }
    }
}
