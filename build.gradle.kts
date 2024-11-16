
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)

    implementation(libs.kotlin.coroutine)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.2.0")
    implementation("org.mongodb:bson-kotlin:5.2.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("io.ktor:ktor-server-core:2.3.4") // Adjust version as needed
    implementation("io.ktor:ktor-server-netty:2.3.4") // For Netty engine
    implementation("io.ktor:ktor-server-auth:2.3.4") // For authentication
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4") // Content negotiation for JSON
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4") // JSON serialization


    implementation("io.ktor:ktor-server-cors:3.0.0-rc-2")

    testImplementation("io.mockk:mockk:1.13.12")
}
