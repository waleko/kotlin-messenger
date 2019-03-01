package ru.kotlin566.messenger.server

import io.ktor.application.*

import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import com.fasterxml.jackson.databind.*
import io.ktor.jackson.*
import io.ktor.features.*
import io.ktor.request.receive
import io.ktor.server.netty.EngineMain
import io.ktor.util.pipeline.PipelineContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Exception

val server = MessengerServer()
val log: Logger = LoggerFactory.getLogger("Application")

fun main(args: Array<String>) {

    // FIXME: добавляем тестовых пользователей и чат для отладки
    debugInit()

    EngineMain.main(args)
}

private fun debugInit() {
    server.usersCreate("pupkin", "Vasiliy Pupkin", "password")
    server.usersCreate("ivanov", "Ivan Ivanov", "123456")

    val token = server.signIn("pupkin", "password")
    log.info("Token for 'pupkin': $token")
    val info = server.chatsCreate("First chat", "pupkin", token)
    log.info("Id of 'First chat': ${info.chatId}")

    val token2 = server.signIn("ivanov", "123456")
    log.info("Token for 'ivanov': $token2")

}

fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {

        // curl -v http://127.0.0.1:9999/v1/health
        get("/v1/health") {
            call.respondText("OK", ContentType.Text.Html)
        }

        // curl -v -X POST "http://127.0.0.1:9999/v1/chats/?_user_id=pupkin&_token=5cae1fec-ce03-48aa-922c-ea082540f772" -H "Content-type: application/json" --data '{ "defaultName": "new chat" }'
        post("/v1/chats/"){
            withAuthorizationParams { userId, token ->
                val info = call.receive<NewChatInfo>()
                val newChat = server.chatsCreate(info.defaultName, userId, token)
                call.respond(newChat)
            }
        }

        // curl -v -X POST "http://127.0.0.1:9999/v1/chats/2/invite?_user_id=pupkin&_token=5cae1fec-ce03-48aa-922c-ea082540f772" -H "Content-type: application/json" --data '{ "userId": "ivanov" }'
        post("/v1/chats/{id}/invite/") {
            withAuthorizationParams { userId, token ->
                val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                val info = call.receive<InviteChatInfo>()
                server.usersInviteToChat(info.userId, chatId, userId, token)
                call.respond(mapOf("status" to "OK"))
            }
        }

        // curl -v -X POST "http://127.0.0.1:9999/v1/chats/2/join?_user_id=ivanov&_token=f74c73e0-5efb-43a7-bf42-030d4ba46483" -H "Content-type: application/json" --data '{ "defaultName": "chat with Pupkin", "secret": "5cae1fec" }'
        post("/v1/chats/{id}/join"){
            withAuthorizationParams { userId, token ->
                val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                val info = call.receive<JoinChatInfo>()
                server.chatsJoin(chatId, info.secret, userId, token, info.defaultName)
                call.respond(mapOf("status" to "OK"))
            }
        }

        // curl -v "http://127.0.0.1:9999/v1/me/chats?_user_id=pupkin&_token=5cae1fec-ce03-48aa-922c-ea082540f772"
        // curl -v "http://127.0.0.1:9999/v1/me/chats?_user_id=ivanov&_token=f74c73e0-5efb-43a7-bf42-030d4ba46483"
        get("/v1/me/chats") {
            withAuthorizationParams { userId, token ->
                val chats = server.usersListChats(userId, token)
                call.respond(chats)
            }
        }

        // curl -v "http://127.0.0.1:9999/v1/chats/2/members?_user_id=pupkin&_token=5cae1fec-ce03-48aa-922c-ea082540f772"
        get("/v1/chats/{id}/members") {
            withAuthorizationParams { userId, token ->
                val chatId = call.parameters["id"] ?.toInt() ?: throw ChatNotFoundException()
                val members = server.chatsMembersList(chatId, userId, token)
                call.respond(members)
            }
        }

        // curl -v -X POST "http://127.0.0.1:9999/v1/chats/2/messages/?_user_id=ivanov&_token=f74c73e0-5efb-43a7-bf42-030d4ba46483" -H "Content-type: application/json" --data '{ "text": "Всем чмоки в этом чате))))" }'
        // curl -v -X POST "http://127.0.0.1:9999/v1/chats/2/messages/?_user_id=pupkin&_token=5cae1fec-ce03-48aa-922c-ea082540f772" -H "Content-type: application/json" --data '{ "text": "превед, медвед" }'
        post("/v1/chats/{id}/messages/") {
            withAuthorizationParams { userId, token ->
                val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                val info = call.receive<NewMessageInfo>()
                val newUser = server.chatMessagesCreate(chatId, info.text, userId, token)
                call.respond(newUser)
            }
        }

        // curl -v -X GET "http://127.0.0.1:9999/v1/chats/1/messages/?_user_id=ivanov&_token=f74c73e0-5efb-43a7-bf42-030d4ba46483"
        // curl -v -X GET "http://127.0.0.1:9999/v1/chats/2/messages/?_user_id=pupkin&_token=5cae1fec-ce03-48aa-922c-ea082540f772"
        get("/v1/chats/{id}/messages/") {
            withAuthorizationParams { userId, token ->
                val chatId = call.parameters["id"] ?.toInt() ?: throw ChatNotFoundException()
                val afterId = call.parameters["after_id"] ?.toInt() ?: 0
                val messages = server.chatMessagesList(chatId, userId, token, afterId)
                call.respond(messages)
            }
        }

        // curl -v -X POST http://127.0.0.1:9999/v1/users/ -H "Content-type: application/json" --data '{ "userId": "petrov", "displayName": "Petr Petrov", "password" : "qwerty" }'
        post("/v1/users/") {
            val info = call.receive<NewUserInfo>()
            val newUser = server.usersCreate(info.userId, info.displayName, info.password)
            call.respond(newUser)
        }

        // curl -v "http://127.0.0.1:9999/v1/users/?_user_id=pupkin&_token=5cae1fec-ce03-48aa-922c-ea082540f772"
        get("/v1/users/") {
            withAuthorizationParams { userId, token ->
                val namePattern = call.parameters["name"]
                val users = server.usersListByName(namePattern,userId, token)
                call.respond(users)
            }
        }

        // curl -v "http://127.0.0.1:9999/v1/users/ivanov?_user_id=pupkin&_token=5cae1fec-ce03-48aa-922c-ea082540f772"
        get("/v1/users/{id}") {
            withAuthorizationParams { userId, token ->
                val userToFindId = call.parameters["id"] ?: throw UserNotFoundException()
                call.respond(server.usersListById(userToFindId, userId, token).first())
            }
        }

        // curl -v "http://127.0.0.1:9999/v1/admin?_user_id=pupkin&_token=5cae1fec-ce03-48aa-922c-ea082540f772"
        get("/v1/admin") {
            withAuthorizationParams { _, _ ->
                call.respond(server.getSystemUser())
            }
        }

        // curl -X POST http://127.0.0.1:9999/v1/users/pupkin/signin --data '{ "password" :"password" }'
        post("/v1/users/{id}/signin") {
            val userId = call.parameters["id"]
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "user id not provided"))
                return@post
            }
            val info = call.receive<PasswordInfo>()
            val token = server.signIn(userId, info.password)
            call.respond(mapOf("token" to token))
        }

        // curl -X POST "http://127.0.0.1:9999/v1/me/signout/?user_id=pupkin&password=password"
        post("/v1/me/signout") {
            withAuthorizationParams { userId, token ->
                call.respond(server.signOut(userId, token))
            }
        }
    }
}

// FIXME: For right implementation use JWT. See, for instance, https://github.com/AndreasVolkmann/ktor-auth-jwt-sample
suspend inline fun PipelineContext<Unit,ApplicationCall>.withAuthorizationParams(function: PipelineContext<Unit,ApplicationCall>.(userId: String, token:String) -> Unit) {
    val userId = call.parameters["_user_id"]
    val token = call.parameters["_token"]
    if (userId == null || token == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "_user_id and _token must be provided"))
        return
    }
    try {
        server.checkUserAuthorization(userId, token)
        function(userId, token)
    }
    catch (e: UserNotMemberException) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "user not authorized"))
    }
    catch (e: UserAlreadyMemberException) {
        call.respond(HttpStatusCode.Conflict, mapOf("error" to "user already member of chat"))
    }
    catch (e: MessageAlreadyExistsException) {
        call.respond(HttpStatusCode.Conflict, mapOf("error" to "message already exists"))
    }
    catch (e: SecretAlreadyExistsException) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal error"))
    }
    catch (e: UserNotAuthorizedException) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "user not authorized"))
    }
    catch (e: UserAlreadyExistsException) {
        call.respond(HttpStatusCode.Conflict, mapOf("error" to "user already exists"))
    }
    catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal error"))
    }
}


