package com.example.data.responses

import kotlinx.serialization.Serializable


@Serializable
data class JobInfoResponse(
    val email: String,
    val usersId: String,
    val countryCode: String,
    val mobile: String,
    val fileUrl: String

)
