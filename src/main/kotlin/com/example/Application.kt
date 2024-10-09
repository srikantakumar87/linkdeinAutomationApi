package com.example


import com.example.data.user.MongoUserDataSource
import com.example.plugins.*
import com.example.security.hashing.SHA256HashingService
import com.example.security.token.JwtTokenService
import com.example.security.token.TokenConfig
import io.ktor.server.application.*
import com.mongodb.kotlin.client.coroutine.MongoClient
import org.slf4j.LoggerFactory


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    val mongoClient = configureMongoDB()
    val userDataSource = MongoUserDataSource(mongoClient.getDatabase("ktor-auth"))
    val tokenConfig = configureToken()
    val tokenService = JwtTokenService()
    val hashingService = SHA256HashingService()

    configureMonitoring()
    configureSerialization()
    configureSecurity(tokenConfig)
    configureRouting(userDataSource, hashingService, tokenService, tokenConfig)
}

// Add a logger for better traceability
private val logger = LoggerFactory.getLogger("Application")

fun Application.configureMongoDB(): MongoClient {
    val mongoPw = System.getenv("MONGO_PW")
        ?: throw IllegalArgumentException("Missing MongoDB password environment variable")
    val mongoUserName = environment.config.property("database.userName").getString()
    val dbName = environment.config.property("database.dbName").getString()
    val uri = "mongodb+srv://$mongoUserName:$mongoPw@cluster0.tc6er.mongodb.net/$dbName?retryWrites=true&w=majority&appName=Cluster0"

    logger.info("Connecting to MongoDB at $dbName with user $mongoUserName")

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
