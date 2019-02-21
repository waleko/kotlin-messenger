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
import ru.kotlin566.messenger.server.UserInfo
import ru.kotlin566.messenger.server.module
import kotlin.test.assertNotNull

class ApplicationTest {

    private val objectMapper = jacksonObjectMapper()

    private val testUser1 = NewUserInfo("pupkin", "Pupkin", "password")
    private val testUser2 = NewUserInfo("teapot2", "Teapot2", "i_am_a_teapot")
    private val testUser3 = NewUserInfo("teapot3", "Teapot3", "i_am_a_teapot")

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

    private val testUserCreation_data = listOf (
            testUser1,
            testUser2,
            testUser3)

    @Test
    fun testUserCreation() {
        withTestApplication({ module() }) {

            // Register
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(testUser1))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val user = objectMapper.readValue<ClientUserInfo>(response.content!!)
                assertEquals(testUser1.userId, user.userId)
                assertEquals(testUser1.displayName, user.displayName)

                // Login
                handleRequest {
                    method = HttpMethod.Post
                    uri = """/v1/users/${testUser1.userId}/singin"""
                    addHeader("Content-type", "application/json")
                    setBody(objectMapper.writeValueAsString(PasswordInfo(testUser1.password)))
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val tokenInfo = objectMapper.readValue<HashMap<String, String>>(response.content!!)
                    val token = tokenInfo["token"]
                    assertNotNull(token)
                    assertTrue(token.length == 36)


                    // Logout
                    handleRequest {
                        method = HttpMethod.Post
                        uri = "/v1/me/singout?_user_id=${testUser1.userId}&_token=$token"
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

    // TODO testChatsLeave: remove normal person from chat, remove non-joined person, remove non-existing person,
    // TODO remove person from their chat with wrong auth, remove non-existing person from non-existing chat.

    // TODO testUsersListChats: list from normal person, list from bad auth, list from non-existing person.

    // TODO testChatsMembersList: list of normal person from chat, list of non-joined person, list of non-existing person,
    // TODO list of person from their chat with wrong auth, list of non-existing person from non-existing chat.

    // TODO testChatMessagesCreate: from normal to normal, from not joined to normal, from normal to non-existing,
    // TODO from non-existing to normal, from normal to normal bad auth, from normal to non-existing bad auth,
    // TODO normal to normal + empty message, normal to normal + giant and very bad message.

    // TODO testChatMessagesList: normal of normal + check with message creation, non-joined to normal, non-existing to normal, normal to normal bad auth,
    // TODO normal to non-existing, non-existing to non-existing, normal to normal afterId < 0.

    // TODO testChatMessagesDeleteById: delete normal of normal + check with chatMessagesList(), delete non-existing message from normal,
    // TODO delete normal from non-existing chat, delete normal from not-joined chat by user, delete normal from non-existing user.

    // TODO testCheckUserAuthorization: normal, bad auth, non-existing.

    // TODO testGetSystemUserId: normal.
}