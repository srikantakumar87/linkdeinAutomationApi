package com.example.plugins

import com.example.authenticateRoute
import com.example.data.user.UserDataSource
import com.example.secretInfoRoute
import com.example.security.hashing.HashingService
import com.example.security.token.TokenConfig
import com.example.security.token.TokenService
import com.example.signIn
import com.example.signUp
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    userDataSource: UserDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig
) {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        signIn(hashingService,userDataSource,tokenService, tokenConfig)
        signUp(hashingService,userDataSource)
        authenticateRoute()
        secretInfoRoute()
    }
}
