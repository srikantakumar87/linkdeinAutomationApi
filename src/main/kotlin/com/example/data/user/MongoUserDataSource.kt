package com.example.data.user

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.example.data.user.User
import com.mongodb.MongoWriteException
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.result.InsertOneResult
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.Document

class MongoUserDataSource(
    db: MongoDatabase
): UserDataSource {

    private val users = db.getCollection<User>("users")

    // Function to ensure a unique index on the email field
    suspend fun initializeEmailIndex() {
        users.createIndex(Document("email", 1), IndexOptions().unique(true))
    }


    override suspend fun getUserByUserName(username: String): User? {
        val filter = Document("email", username)
        return users.find(filter).firstOrNull()
    }

    override suspend fun insertUser(user: User): Boolean {

        initializeEmailIndex()
        return try {
            val result: InsertOneResult = users.insertOne(user)
            println("User inserted successfully: $result")
            result.wasAcknowledged()
        } catch (e: MongoWriteException) {
            if (e.code == 11000) { // Duplicate key error code
                println("Error: Email '${user.email}' already exists.")
                false
            } else {
                println("An error occurred while inserting user: ${e.message}")
                false
            }
        }
    }

}