package ru.kotlin566.messenger.client

import ru.kotlin566.messenger.server.*
import java.io.IOException
import org.springframework.security.crypto.keygen.KeyGenerators.string
//import com.sun.corba.se.spi.presentation.rmi.StubAdapter.request
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
//        val formBody: RequestBody = FormBody.Builder()
//                .add("message", "Your message")
//                .build()
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

    fun singIn(userId: String, password: String): User {
        val token = server.singIn(userId, password)
        return User(userId, token, this)
    }

    fun singOut(userId: String, token: String) {
        server.singOut(userId, token)
    }

    fun usersListById(userIdToFind: String, userId: String, token: String): List<UserInfo> {
        return server.usersListById(userIdToFind, userId, token)
    }

    fun usersListChats(userId: String, token: String): List<ChatInfo> {
        return server.usersListChats(userId, token)
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
        return server.chatsMembersList(chatId, userId, token)
    }

    fun chatMessagesList(chatId: Int, userId: String, token: String): List<MessageInfo> {
        return server.chatMessagesList(chatId, userId, token)
    }
}

open class ClientAware(val client: MessengerClient)
open class UserAware(val user: User) : ClientAware(user.client)
open class ChatAware(chat: Chat) : UserAware(chat.user)