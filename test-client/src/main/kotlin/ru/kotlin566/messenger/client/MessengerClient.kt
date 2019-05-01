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

    fun checkAlive() {
        val request = Request.Builder()
                .url("$PATH/v1/health")
                .get()
                .build()
        try {
            val response = client.newCall(request).execute()

            val serverAnswer = response.body().string()

            if (serverAnswer == null) {
                println("Bad health, test answer is null.")     //TODO: to work on getting answers!
            }
            println(serverAnswer.toString())

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun register(login: String, name: String, password: String) {
        val jsonObject = JSONObject();
        jsonObject.put("userId", login)
        jsonObject.put("displayName", name)
        jsonObject.put("password", password)
        val jsonString = jsonObject.toString();
        val body = RequestBody.create(JSON_T, jsonString)
        val request = Request.Builder()
                .url("$PATH/v1/users/")
                .post(body)
                .build()
        try {
            val response = client.newCall(request).execute()

            val serverAnswer = response.body().string()

            if (serverAnswer == null) {
                println("Smthng bd, answer is null.")           //TODO: Error codes!
            }
            println(serverAnswer.toString())

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun signIn(userId: String, password: String): User {
        val jsonObject = JSONObject()
        jsonObject.put("password", password)
        val jsonString = jsonObject.toString()
        val body = RequestBody.create(JSON_T, jsonString)
        val request = Request.Builder()
                .url("$PATH/v1/users/$userId/signin")
                .post(body)
                .build()
        try {
            val response = client.newCall(request).execute()

            val serverAnswer = response.body().string()

            val mapper = jacksonObjectMapper()

            if (serverAnswer == null) {
                println("Smthng bd, answer is null.")           //TODO: Error codes!
            }

            val token = mapper.readValue<Token>(serverAnswer.toString())
            println(serverAnswer.toString())
            println(token)
            return User(userId, token.token, this)

        } catch (e: IOException) {
            e.printStackTrace()
            return User("NULL", "NULL", this)   //FIXME: NullUser
        }
    }

    fun signOut(userId: String, token: String) {
        server.signOut(userId, token)
    }

    fun usersListById(userIdToFind: String, userId: String, token: String): List<UserInfo> {
        val request = Request.Builder()
                .url("$PATH/v1/users/$userId")
                .addHeader("Authorization: Bearer ", token)
                .get()
                .build()
        try {
            val response = client.newCall(request).execute()

            val serverAnswer = response.body().string()

            val mapper = jacksonObjectMapper()

            if (serverAnswer == null) {
                println("Smthng bd, answer is null.")           //TODO: Error codes!
            }
            println(serverAnswer.toString())

            return listOf(mapper.readValue<UserInfo>(serverAnswer.toString()))

        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun usersListChats(userId: String, token: String): List<ChatInfo> {
        val request = Request.Builder()
                .url("$PATH/v1/me/chats")
                .addHeader("Authorization: Bearer ", token)
                .get()
                .build()
        try {
            val response = client.newCall(request).execute()

            val serverAnswer = response.body().string()

            val mapper = jacksonObjectMapper()

            if (serverAnswer == null) {
                println("Smthng bd, answer is null.")           //TODO: Error codes!
            }
            println(serverAnswer.toString())

            return mapper.readValue<List<ChatInfo>>(serverAnswer.toString())

        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }
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
        val request = Request.Builder()
                .url("$PATH/v1/chats/$chatId/members")
                .addHeader("Authorization: Bearer ", token)
                .get()
                .build()
        try {
            val response = client.newCall(request).execute()

            val serverAnswer = response.body().string()

            val mapper = jacksonObjectMapper()

            if (serverAnswer == null) {
                println("Smthng bd, answer is null.")           //TODO: Error codes!
            }
            println(serverAnswer.toString())

            return mapper.readValue<List<MemberInfo>>(serverAnswer.toString())

        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }
    }


    fun chatMessagesList(chatId: Int, userId: String, token: String): List<MessageInfo> {
        val request = Request.Builder()
                .url("$PATH/v1/chats/$chatId/messages")
                .addHeader("Authorization: Bearer ", token)
                .get()
                .build()
        try {
            val response = client.newCall(request).execute()

            val serverAnswer = response.body().string()

            val mapper = jacksonObjectMapper()

            if (serverAnswer == null) {
                println("Smthng bd, answer is null.")           //TODO: Error codes!
            }
            println(serverAnswer.toString())

            return mapper.readValue<List<MessageInfo>>(serverAnswer.toString())

        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }
    }
}

open class ClientAware(val client: MessengerClient)
open class UserAware(val user: User) : ClientAware(user.client)
open class ChatAware(chat: Chat) : UserAware(chat.user)