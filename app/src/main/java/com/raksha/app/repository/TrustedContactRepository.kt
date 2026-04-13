package com.raksha.app.repository

import com.raksha.app.data.local.dao.TrustedContactDao
import com.raksha.app.data.local.entity.TrustedContactEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustedContactRepository @Inject constructor(
    private val contactDao: TrustedContactDao
) {
    val contacts: Flow<List<TrustedContactEntity>> = contactDao.getContacts()

    suspend fun getContactsOnce(): List<TrustedContactEntity> = contactDao.getContactsOnce()

    suspend fun getContactCount(): Int = contactDao.getContactCount()

    suspend fun addContact(name: String, phone: String): Boolean {
        val count = contactDao.getContactCount()
        if (count >= 5) return false
        contactDao.insertContact(TrustedContactEntity(name = name, phone = phone))
        return true
    }

    suspend fun deleteContact(contact: TrustedContactEntity) {
        contactDao.deleteContact(contact)
    }

    suspend fun deleteContactById(id: Int) {
        contactDao.deleteContactById(id)
    }
}
