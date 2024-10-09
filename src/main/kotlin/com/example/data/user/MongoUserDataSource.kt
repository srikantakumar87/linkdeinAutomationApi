package com.example.data.user

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.example.data.user.User
import com.mongodb.client.result.InsertOneResult
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document

class MongoUserDataSource(
    db: MongoDatabase
): UserDataSource {

    private val users = db.getCollection<User>("users")

    override suspend fun getUserByUserName(username: String): User? {
        val filter = Document("username", username)
        return users.find(filter).firstOrNull()
    }

    override suspend fun insertUser(user: User): Boolean {

        val result: InsertOneResult = users.insertOne(user)
        return result.wasAcknowledged()
    }

}