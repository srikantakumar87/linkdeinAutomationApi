package com.example.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class JobInfoRetrieveByUserId(
    var userId: String
)
