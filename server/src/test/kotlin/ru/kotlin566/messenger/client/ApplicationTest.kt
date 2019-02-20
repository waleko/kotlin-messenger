package ru.kotlin566.messenger.client

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Assert.assertTrue
import ru.kotlin566.messenger.server.NewUserInfo
import ru.kotlin566.messenger.server.PasswordInfo
import ru.kotlin566.messenger.server.module
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
    fun testUserCreation_UserAlreadyExists(){

    }

    @Test
    fun `testLogin&Logout`() {

    }

    @Test
    fun testUserCreation() {
        // TODO add separate login and logout test function
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
                    uri = "/v1/users/pupkin/singin"
                    addHeader("Content-type", "application/json")
                    setBody(objectMapper.writeValueAsString(PasswordInfo("password")))
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val tokenInfo = objectMapper.readValue<HashMap<String, String>>(response.content!!)
                    val token = tokenInfo["token"]
                    assertNotNull(token)
                    assertTrue(token.length == 36)


                    // Logout
                    handleRequest {
                        method = HttpMethod.Post
                        uri = "/v1/me/singout?_user_id=pupkin&_token=$token"
                    }.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                    }
                }
            }
        }
    }

    // TODO testUsersListById: my user, other user, non-existing user, you are non-existing user, wrong auth token.

    // TODO testUsersListByName: my user, other user, non-existing user, you are non-existing user, wrong auth token.

    // TODO testDeleteUser: delete user, delete non-existing user, delete with using wrong auth.

    // TODO testChatsCreate: create normal chat, create existing chat, create with non-existing user, create with bad auth.

    // TODO testUsersInviteToChat: invite normal person, invite yourself, invite already invited person, invite non-existing person,
    // TODO invite normal with bad auth, invite kicked person.

    // TODO testChatsJoin: invite normal person to existing chat, invite already joined person to a chat,
    // TODO invite normal person to non-existing chat, invite non-existing person to chat, invite normal person to chat with wrong secret,
    // TODO invite normal person with wrong auth to chat, invite non-existing person to non-existing chat, invite normal person with wrong auth to chat with wrong secret.

    // TODO testChatsLeave: remove normal person from chat, remove non-joined person, remove non-existing person, remove person from not their chat,

    // TODO: test branches
}