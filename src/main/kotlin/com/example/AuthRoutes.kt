package com.example

import com.example.data.requests.AuthRequest
import com.example.data.responses.AuthResponse
import com.example.data.user.User
import com.example.data.user.UserDataSource
import com.example.security.hashing.HashingService
import com.example.security.hashing.SaltedHash
import com.example.security.token.TokenClaim
import com.example.security.token.TokenConfig
import com.example.security.token.TokenService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.apache.commons.codec.digest.DigestUtils
import kotlin.runCatching

fun Route.signUp(
    hashingService: HashingService,
    userDataSource: UserDataSource
) {
    post("signUp") {
        // Explicitly specifying the type in runCatching
        val request = runCatching { call.receive<AuthRequest>() }.getOrNull()
        if (request == null || !isRequestValid(request)) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        val saltedHash = hashingService.generateSaltedHash(request.password)
        val user = createUser(request.username, saltedHash.hash, saltedHash.salt)

        if (!userDataSource.insertUser(user)) {
            call.respond(HttpStatusCode.Conflict)
            return@post
        }

        call.respond(HttpStatusCode.OK , message = "user successfully created")
    }
}

fun Route.signIn(
    hashingService: HashingService,
    userDataSource: UserDataSource,
    tokenService: TokenService,
    tokenConfig: TokenConfig
) {
    post("signIn") {
        val request = runCatching { call.receive<AuthRequest>() }.getOrNull()

        if (request == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        val user = userDataSource.getUserByUserName(request.username)
        if (user == null) {
            call.respond(HttpStatusCode.Conflict, message = "Incorrect user name")
            return@post
        }


        val isValidPassword = hashingService.verify(
            value = request.password,
            saltedHash = SaltedHash(
                hash = user.password,
                salt = user.salt
            )
        )

        if(!isValidPassword){



            val d = DigestUtils.sha256Hex("${user.salt}${request.password}")
            val h = user.password



            //call.respond(HttpStatusCode.Conflict, message = "Incorrect password user hash $h provided hash $d")
            call.respond(HttpStatusCode.Conflict, message = "Incorrect password")
            return@post

        }
        val token = tokenService.generate(
            config = tokenConfig,
            TokenClaim(
                name = "userId",
                value = user.id.toString()
            )
        )

        call.respond(
            status = HttpStatusCode.OK,
            message = AuthResponse(
                token = token
            )
        )

    }
}
fun Route.authenticateRoute() {
    authenticate("auth-bearer"){
        get("/authenticate") {
            call.respond(HttpStatusCode.OK, "Authenticated")
        }
    }
}

fun Route.secretInfoRoute() {
    authenticate("auth-bearer"){
        get("/secret") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("userId", String::class) ?: run {
                call.respond(HttpStatusCode.Unauthorized, "Missing user ID")
                return@get
            }
            call.respond(HttpStatusCode.OK, "Your userId is $userId")
        }
    }
}

private fun isRequestValid(request: AuthRequest): Boolean {
    return request.username.isNotBlank() && request.password.length >= 8
}

private fun createUser(username: String, hashedPassword: String, salt: String): User {
    return User(
        username = username,
        password = hashedPassword,
        salt = salt
    )
}