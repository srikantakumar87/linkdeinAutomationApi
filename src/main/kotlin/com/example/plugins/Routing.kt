package com.example.plugins

import com.example.authenticateRoute
import com.example.data.jobInfo.JobInfoDataSource
import com.example.data.user.UserDataSource
import com.example.jobInfoCreate
import com.example.jobInfoRetrieve
import com.example.secretInfoRoute
import com.example.security.hashing.HashingService
import com.example.security.token.TokenConfig
import com.example.security.token.TokenService
import com.example.signIn
import com.example.signUp
import io.ktor.http.ContentDisposition.Companion.File
import io.ktor.server.application.*
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting(
    userDataSource: UserDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig,
    jobInfoDataSource: JobInfoDataSource
) {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        staticFiles("/uploads", File("uploads"))

        signIn(hashingService,userDataSource,tokenService, tokenConfig)
        signUp(hashingService,userDataSource)
        authenticateRoute()
        secretInfoRoute()
        jobInfoCreate(jobInfoDataSource)
        jobInfoRetrieve(jobInfoDataSource)
    }
}
