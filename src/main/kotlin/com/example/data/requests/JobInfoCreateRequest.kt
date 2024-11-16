package com.example.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class JobInfoCreateRequest(
    val email: String,
    val userId: String,
    val countryCode: String,
    val mobile: String,
    //val fileUrl: String? = null

)