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
    val canAddMoreContacts: Boolean = true
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
            contactRepository.addContact(name.trim(), phone.trim())
            _uiState.value = _uiState.value.copy(showAddContactDialog = false)
        }
    }

    fun deleteContact(contact: TrustedContactEntity) {
        viewModelScope.launch { contactRepository.deleteContact(contact) }
    }

    fun showAddContactDialog() {
        _uiState.value = _uiState.value.copy(showAddContactDialog = true)
    }

    fun dismissAddContactDialog() {
        _uiState.value = _uiState.value.copy(showAddContactDialog = false)
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
}
