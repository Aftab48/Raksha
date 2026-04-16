package com.raksha.app.feature_trusted_contacts.data.remote.api

import com.raksha.app.feature_trusted_contacts.data.remote.dto.SaveTrustedContactRequestDto
import com.raksha.app.feature_trusted_contacts.data.remote.dto.TrustedContactsResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UserTrustedContactsAPI {
    @POST("user/save-trusted-contacts")
    suspend fun saveTrustedContact(
        @Body request: SaveTrustedContactRequestDto
    ): TrustedContactsResponseDto

    @GET("user/get-trusted-contacts")
    suspend fun getTrustedContacts(): TrustedContactsResponseDto

    @DELETE("user/trusted-contact/{contactPhoneNumber}")
    suspend fun deleteTrustedContact(
        @Path("contactPhoneNumber") contactPhoneNumber: String
    ): TrustedContactsResponseDto
}
