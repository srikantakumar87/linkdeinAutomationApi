package com.example


import com.example.data.jobInfo.MongoJobInfoDataSource
import com.example.data.user.MongoUserDataSource
import com.example.plugins.*
import com.example.security.hashing.SHA256HashingService
import com.example.security.token.JwtTokenService
import com.example.security.token.TokenConfig
import io.ktor.server.application.*
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.netty.EngineMain
import org.slf4j.LoggerFactory
import io.ktor.server.plugins.cors.routing.CORS


fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {

    val mongoClient = configureMongoDB()
    val dbName = environment.config.property("database.dbName").getString()
    val userDataSource = MongoUserDataSource(mongoClient.getDatabase(dbName))

    val jobInfoDataSource = MongoJobInfoDataSource(mongoClient.getDatabase(dbName))
    val tokenConfig = configureToken()
    val tokenService = JwtTokenService()
    val hashingService = SHA256HashingService()

    configureMonitoring()
    configureSerialization()
    configureCORS() // Enable CORS
    configureSecurity(tokenConfig)
    configureRouting(
        userDataSource, hashingService, tokenService, tokenConfig,
        jobInfoDataSource
    )
}

// Add a logger for better traceability
private val logger = LoggerFactory.getLogger("Application")

fun Application.configureMongoDB(): MongoClient {
    val cMongoPw = System.getenv("C_MONGO_PW")
        ?: throw IllegalArgumentException("Missing MongoDB password environment variable")
    val mongoUserName = environment.config.property("database.userName").getString()
    val cUserName = environment.config.property("database.cuserName").getString()
    val dbName = environment.config.property("database.dbName").getString()
    val dbDomain = environment.config.property("database.domain").getString()
    val dbPort = environment.config.property("database.port").getString()
    val uri = "mongodb://$cUserName:$cMongoPw@$dbDomain:$dbPort/?authMechanism=SCRAM-SHA-1"
    logger.info("Connecting to MongoDB at ktor with user srikanta")
    return MongoClient.create(uri)
}

// Extract JWT token configuration into a separate function
fun Application.configureToken(): TokenConfig {
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val realm = environment.config.property("jwt.realm").getString()
    val secret = System.getenv("JWT_SECRET")
        ?: throw IllegalArgumentException("Missing JWT secret environment variable")

    logger.info("Configuring JWT token with issuer $issuer and audience $audience")

    return TokenConfig(
        issuer = issuer,
        audience = audience,
        expiresIn = 365L * 1000L * 60L * 60L * 24L,
        secret = secret,
        realm = realm
    )
}
// CORS Configuration function
fun Application.configureCORS() {
    install(CORS) {
        anyHost()
        //allowHost("localhost:8585")  // Allow specific host (e.g., frontend on localhost:3000)
        //allowHost("example.com")      // Add more allowed hosts as needed

        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization) // Allow authorization header for token-based auth

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)

        allowCredentials = true // Allow sending credentials (like cookies) with requests
    }
}
