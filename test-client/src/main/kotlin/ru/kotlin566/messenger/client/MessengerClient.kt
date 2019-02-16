package ru.kotlin566.messenger.client

import okhttp3.FormBody
import ru.kotlin566.messenger.server.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import org.springframework.security.crypto.keygen.KeyGenerators.string
import com.sun.corba.se.spi.presentation.rmi.StubAdapter.request



/**
 * Клиент мессенджера
 */
class MessengerClient(private val server: MessengerServer) {

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
                println("Bad health, test answer is null.")
            }
            println(serverAnswer.toString())


        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun register(login: String, name: String, password: String) {
        server.usersCreate(login, name, password)
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