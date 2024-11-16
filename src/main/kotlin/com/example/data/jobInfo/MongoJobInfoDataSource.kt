package com.example.data.jobInfo

import com.mongodb.client.result.InsertOneResult
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document

class MongoJobInfoDataSource(
    db: MongoDatabase
) : JobInfoDataSource {

    private val jobInfos = db.getCollection<JobInfo>("jobInfo")



    override suspend fun insertJobInfo(jobInfo: JobInfo): Boolean {
        val result: InsertOneResult = jobInfos.insertOne(jobInfo)
        return result.wasAcknowledged()
    }

    override suspend fun getJobInfo(usersId: String): JobInfo? {

        val filter = Document("usersId", usersId)
        return jobInfos.find(filter).firstOrNull()
    }
}