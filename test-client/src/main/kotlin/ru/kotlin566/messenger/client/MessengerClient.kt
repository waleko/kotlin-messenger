package ru.kotlin566.messenger.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ru.kotlin566.messenger.server.*
import java.io.IOException
import org.springframework.security.crypto.keygen.KeyGenerators.string
import okhttp3.*
import okhttp3.RequestBody
import org.json.simple.JSONObject


/**
 * Клиент мессенджера
 */
class MessengerClient(private val server: MessengerServer) {

    val JSON_T = MediaType.parse("application/json; charset=utf-8")
    val PATH: String = "http://127.0.0.1:9999"                 //TODO: it's a local address
    val client = OkHttpClient()
    val mapper = jacksonObjectMapper()

    fun makeRequest(args: Map<Any, Any>, headers: Map<String, String>, isPost: Boolean, url: String): String? {
        val requestB = Request.Builder()
                .url(url)
        for (header in headers) {
            requestB.addHeader(header.key, header.value)
        }
        if (isPost) {
            val jsonObject = JSONObject()
            for (item in args) {
                jsonObject.put(item.key, item.value)
            }
            val jsonString = jsonObject.toString();
            val body = RequestBody.create(JSON_T, jsonString)
            requestB.post(body)
        } else {
            requestB.get()
        }
        val request = requestB.build()

        try {
            val response = client.newCall(request).execute()
            return response.body().string()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun checkAlive() {
        val serverAnswer = makeRequest(emptyMap(), emptyMap(), false, "$PATH/v1/health")
        println(serverAnswer)
    }

    fun register(login: String, name: String, password: String) {
        val serverAnswer = makeRequest(mapOf("userId" to login, "displayName" to name, "password" to password), emptyMap(), true, "$PATH/v1/users/")

        if (serverAnswer == null) {
            println("Smthng bd, answer is null.")           //TODO: Error codes!
        }
        println(serverAnswer.toString())
    }

    fun signIn(userId: String, password: String): User {
        val serverAnswer = makeRequest(mapOf("password" to password), emptyMap(), true, "$PATH/v1/users/$userId/signin")

        if (serverAnswer == null) {
            println("Smthng bd, answer is null.")           //TODO: Error codes!
        }

        val token = mapper.readValue<Token>(serverAnswer.toString())
        println(serverAnswer.toString())
        println(token)
        return User(userId, token.token, this)

    }

    fun signOut(userId: String, token: String) {
        server.signOut(userId, token)
    }

    fun usersListById(userIdToFind: String, userId: String, token: String): List<UserInfo> {
        val serverAnswer = makeRequest(emptyMap(), mapOf("Authorization: Bearer " to token), false, "$PATH/v1/users/$userId")

        if (serverAnswer == null) {
            println("Smthng bd, answer is null.")           //TODO: Error codes!
        }
        println(serverAnswer.toString())

        return listOf(mapper.readValue<UserInfo>(serverAnswer.toString()))
    }

    fun usersListChats(userId: String, token: String): List<ChatInfo> {
        val serverAnswer = makeRequest(emptyMap(), mapOf("Authorization: Bearer " to token), false, "$PATH/v1/me/chats")

        if (serverAnswer == null) {
            println("Smthng bd, answer is null.")           //TODO: Error codes!
        }
        println(serverAnswer.toString())

        return mapper.readValue<List<ChatInfo>>(serverAnswer.toString())
    }


    fun getSystemUserId(): String {
        return server.getSystemUserId()
    }

    fun chatsCreate(chatName: String, userId: String, token: String): ChatInfo {
        return server.chatsCreate(chatName, userId, token)
    }

    fun chatsJoin(chatId: Int, secret: String, userId: String, token: String, chatName: String? = null) {
        server.chatsJoin(chatId, secret, userId, token, chatName)
    }

    fun usersInviteToChat(userIdToInvite: String, chatId: Int, userId: String, token: String) {
        server.usersInviteToChat(userIdToInvite, chatId, userId, token)
    }

    fun chatMessagesCreate(chatId: Int, text: String, userId: String, token: String): MessageInfo {
        return server.chatMessagesCreate(chatId, text, userId, token)
    }

    fun chatsMembersList(chatId: Int, userId: String, token: String): List<MemberInfo> {
        val serverAnswer = makeRequest(emptyMap(), mapOf("Authorization: Bearer " to token), false, "$PATH/v1/chats/$chatId/members")

        if (serverAnswer == null) {
            println("Smthng bd, answer is null.")           //TODO: Error codes!
        }
        println(serverAnswer.toString())

        return mapper.readValue<List<MemberInfo>>(serverAnswer.toString())
    }


    fun chatMessagesList(chatId: Int, userId: String, token: String): List<MessageInfo> {
        val serverAnswer = makeRequest(emptyMap(), mapOf("Authorization: Bearer " to token), false, "$PATH/v1/chats/$chatId/messages")

        if (serverAnswer == null) {
            println("Smthng bd, answer is null.")           //TODO: Error codes!
        }
        println(serverAnswer.toString())

        return mapper.readValue<List<MessageInfo>>(serverAnswer.toString())

    }
}

open class ClientAware(val client: MessengerClient)
open class UserAware(val user: User) : ClientAware(user.client)
open class ChatAware(chat: Chat) : UserAware(chat.user)