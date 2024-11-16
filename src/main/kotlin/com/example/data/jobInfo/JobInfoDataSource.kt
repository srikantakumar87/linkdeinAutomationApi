package com.example.data.jobInfo

interface JobInfoDataSource{
    suspend fun insertJobInfo(jobInfo: JobInfo): Boolean
    suspend fun getJobInfo(usersId: String): JobInfo?
}