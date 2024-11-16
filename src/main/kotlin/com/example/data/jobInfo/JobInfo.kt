package com.example.data.jobInfo

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class JobInfo(
    val email: String,
    val usersId: String,
    val countryCode: String,
    val mobile: String,
    val fileUrl: String? = null

)