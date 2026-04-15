package com.example.raksha.feature_trusted_contacts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    val name: String,
    @PrimaryKey val phoneNumber: String,
    val priority: Int
)
