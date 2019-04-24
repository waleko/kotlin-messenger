package ru.kotlin566.messenger.server

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.testing.TestApplicationRequest
import kotlin.test.assertNotNull

class ApplicationTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun testHealth() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/v1/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK", response.content)
            }
        }
    }

    data class ClientUserInfo(val userId: String, val displayName: String)

    @Test
    fun testUserCreateSignInSignOut() {
        val userData = NewUserInfo("pupkin", "Pupkin", "password")
        withTestApplication({ module() }) {

            // Register
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(userData))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val user = objectMapper.readValue<ClientUserInfo>(response.content!!)
                assertEquals(userData.userId, user.userId)
                assertEquals(userData.displayName, user.displayName)

                // Login
                handleRequest {
                    method = HttpMethod.Post
                    uri = "/v1/users/pupkin/signin"
                    addHeader("Content-type", "application/json")
                    setBody(objectMapper.writeValueAsString(PasswordInfo("password")))
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val tokenInfo = objectMapper.readValue<HashMap<String,String>>(response.content!!)
                    val token = tokenInfo["token"]
                    assertNotNull(token)
                    println("Token = $token")
                    // Logout
                    handleRequest {
                        method = HttpMethod.Post
                        uri = "/v1/me/signout"
                        addJwtHeader(token)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                    }
                }
            }
        }
    }

    private fun TestApplicationRequest.addJwtHeader(token: String) = addHeader("Authorization", "Bearer $token")
}
