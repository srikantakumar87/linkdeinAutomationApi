package com.example

import com.example.data.jobInfo.JobInfo
import com.example.data.jobInfo.JobInfoDataSource
import com.example.data.requests.AuthRequest
import com.example.data.requests.JobInfoCreateRequest
import com.example.data.requests.JobInfoRetrieveByUserId
import com.example.data.responses.AuthResponse
import com.example.data.responses.JobInfoResponse
import com.example.data.user.User
import com.example.data.user.UserDataSource
import com.example.security.hashing.HashingService
import com.example.security.hashing.SaltedHash
import com.example.security.token.TokenClaim
import com.example.security.token.TokenConfig
import com.example.security.token.TokenService
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import kotlin.runCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.json.Json


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
        val user = createUser(request.email, saltedHash.hash, saltedHash.salt)

        if (!userDataSource.insertUser(user)) {
            call.respond(HttpStatusCode.Conflict, message = "Error: Email '${user.email}' already exists.")
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

        val user = userDataSource.getUserByUserName(request.email)
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

fun Route.jobInfoRetrieve(
    jobInfoDataSource: JobInfoDataSource
) {
    authenticate("auth-bearer") {
        post("jobInfoGet") {
            val request = runCatching { call.receive<JobInfoRetrieveByUserId>() }.getOrNull()
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val jobInfo = jobInfoDataSource.getJobInfo(request.userId)
            print(jobInfo)


            if (jobInfo == null) {
                call.respond(HttpStatusCode.Conflict, message = "userId not found")
                return@post
            }

            call.respond(
                status = HttpStatusCode.OK,
                message = JobInfoResponse(
                    email = jobInfo.email,
                    usersId = jobInfo.usersId,
                    countryCode = jobInfo.countryCode,
                    mobile = jobInfo.mobile,
                    fileUrl = jobInfo.fileUrl.toString(),
                )
            )



        }

    }
}




fun Route.jobInfoCreate(
    jobInfoDataSource: JobInfoDataSource
) {
    authenticate("auth-bearer") {
        post("jobInfoCreate") {
            val multipart = call.receiveMultipart()
            var mutableRequest: JobInfoCreateRequest? = null
            var fileUrl: String? = null
            var errorMessage: String? = null // To capture error messages

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "jobInfoData") {
                            mutableRequest = try {
                                Json.decodeFromString(
                                    JobInfoCreateRequest.serializer(),
                                    part.value
                                )
                            } catch (e: Exception) {
                                errorMessage = "Invalid job info data format."
                                null
                            }
                        }
                    }
                    is PartData.FileItem -> {
                        if (part.name == "file") {
                            val originalFileName = part.originalFileName ?: "uploaded_file"

                            if (!originalFileName.endsWith(".pdf", ignoreCase = true)) {
                                errorMessage = "Only PDF files are allowed."
                                part.dispose()
                                return@forEachPart
                            }

                            val userId = mutableRequest?.userId ?: "default_user"
                            fileUrl = handleFileUpload(part, userId)
                        }
                    }
                    else -> Unit
                }
                part.dispose()
            }

            // Respond with any error if occurred
            errorMessage?.let {
                call.respond(HttpStatusCode.BadRequest, it)
                return@post
            }

            // Make request immutable after parsing
            val request = mutableRequest

            // Check if request data is null
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, message = "Invalid request data.")
                return@post
            }

            // Create JobInfo instance with safe `request` access
            val jobInfo = createJobInfo(
                email = request.email,
                userId = request.userId,
                countryCode = request.countryCode,
                mobile = request.mobile,
                fileUrl = fileUrl
            )

            // Insert job info and handle any conflict
            val isInserted = jobInfoDataSource.insertJobInfo(jobInfo)
            if (!isInserted) {
                call.respond(HttpStatusCode.Conflict, "Failed to create job info; possible duplicate entry or conflict.")
                return@post
            }

            call.respond(HttpStatusCode.Created, jobInfo)
        }
    }
}

// Separate function for file handling
suspend fun handleFileUpload(part: PartData.FileItem, userId: String): String? = withContext(Dispatchers.IO) {
    val uploadDir = File("uploads/$userId")
    if (!uploadDir.exists()) uploadDir.mkdirs()

    val fileName = "${uploadDir.path}/${part.originalFileName ?: "uploaded_file"}"
    val file = File(fileName)

    file.outputStream().use { outputStream ->
        val channel = part.provider()
        val buffer = ByteArray(1024 * 8) // 8 KB buffer
        while (!channel.isClosedForRead) {
            val bytesRead = channel.readAvailable(buffer)
            if (bytesRead == -1) break
            outputStream.write(buffer, 0, bytesRead)
        }
    }
    file.path
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
    return request.email.isNotBlank() && request.password.length >= 8
}

private fun createUser(email: String, hashedPassword: String, salt: String): User {
    return User(
        email = email,
        password = hashedPassword,
        salt = salt
    )
}

private fun createJobInfo(email: String, userId: String, countryCode: String, mobile: String, fileUrl: String?): JobInfo {
    return JobInfo(
        email = email,
        usersId = userId,
        countryCode = countryCode,
        mobile = mobile,
        fileUrl = fileUrl

    )
}