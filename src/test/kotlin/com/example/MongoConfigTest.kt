package com.example

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.ktor.server.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MongoConfigTest {

    @Test
    fun `configureMongoDB throws IllegalArgumentException when MONGO_PW is missing`() = testApplication {
        mockkStatic(System::class)
        every { System.getenv("MONGO_PW") } returns null

        val exception = assertThrows<IllegalArgumentException> {
            application { configureMongoDB() }
        }

        assertEquals("Missing MongoDB password environment variable", exception.message)
    }

    @Test
    fun `configureMongoDB returns valid MongoClient with correct credentials`() = testApplication {
        // Mock environment variables and config
        mockkStatic(System::class)
        every { System.getenv("MONGO_PW") } returns "test-password"

        // Mock ApplicationConfig and ApplicationConfigValue
        val mockConfig = mockk<ApplicationConfig>(relaxed = true)
        val mockUserNameConfigValue = mockk<ApplicationConfigValue>()
        val mockDbNameConfigValue = mockk<ApplicationConfigValue>()

        every { mockUserNameConfigValue.getString() } returns "test-user"
        every { mockDbNameConfigValue.getString() } returns "ktor-auth"
        every { mockConfig.property("database.userName") } returns mockUserNameConfigValue
        every { mockConfig.property("database.dbName") } returns mockDbNameConfigValue

        environment {
            config = mockConfig
        }

        application {
            val client = configureMongoDB()
            assertNotNull(client)
            assertTrue(client is MongoClient)
        }
    }
}