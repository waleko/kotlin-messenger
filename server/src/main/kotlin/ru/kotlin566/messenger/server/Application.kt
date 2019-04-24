package ru.kotlin566.messenger.server

import io.ktor.application.*

import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import com.fasterxml.jackson.databind.*
import io.ktor.auth.*
import io.ktor.auth.jwt.jwt
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

data class AuthInfo(val user: UserInfo, val sessionToken: String) : Principal
val ApplicationCall.user get() = authentication.principal<AuthInfo>()?.user
val ApplicationCall.sessionToken get() = authentication.principal<AuthInfo>()?.sessionToken

fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(CallLogging)
    install(Authentication) {
        /**
         * Setup the JWT authentication to be used in [Routing].
         * If the token is valid, the corresponding [UserInfo] is fetched from the database.
         * The [UserInfo] can then be accessed in each [ApplicationCall].
         */
        jwt {
            verifier(JwtConfig.verifier)
            realm = "kotlin-messenger"
            validate {
                // check secret and return user
                val userId = it.payload.getClaim("id").asString()
                val sessionToken = it.payload.getClaim("session").asString()
                try {
                    server.checkUserAuthorization(userId, sessionToken)
                    val user = server.getUserById(userId)
                    return@validate AuthInfo(user, sessionToken)
                }
                catch (e: UserNotAuthorizedException) {
                    return@validate null
                }
            }
        }
    }

    routing {

        // curl -v -X POST http://127.0.0.1:9999/v1/users/ --data '{ "userId": "petrov", "displayName": "Petr Petrov", "password" : "qwerty" }' -H "Content-type: application/json"
        post("/v1/users/") {
            val info = call.receive<NewUserInfo>()
            val newUser = server.usersCreate(info.userId, info.displayName, info.password)
            call.respond(newUser)
        }

        // curl -X POST http://127.0.0.1:9999/v1/users/pupkin/signin --data '{ "password" :"password" }' -H "Content-type: application/json"
        post("/v1/users/{id}/signin") {
            val userId = call.parameters["id"]
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "user id not provided"))
                return@post
            }
            val info = call.receive<PasswordInfo>()
            val secret = server.signIn(userId, info.password)
            val token = JwtConfig.makeToken(userId, secret)
            call.respond(mapOf("token" to token))
        }

        // curl -v http://127.0.0.1:9999/v1/health
        get("/v1/health") {
            call.respondText("OK", ContentType.Text.Html)
        }

        authenticate {
            // curl -v -X POST "http://127.0.0.1:9999/v1/chats/" --data '{ "defaultName": "new chat" }' -H "Content-type: application/json" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiI1Y2FlMWZlYy1jZTAzLTQ4YWEtOTIyYy1lYTA4MjU0MGY3NzIiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJwdXBraW4ifQ.MiueK0xGBsHR3dWFyPVo2W5FZ9x3zfikz_bfwFvp3vruMwY5Ots5AOnlwjHXGhzBsI9rpZB2U85_3xas3DzelQ"
            post("/v1/chats/"){
                withAuthorizationParams { userId, token ->
                    val info = call.receive<NewChatInfo>()
                    val newChat = server.chatsCreate(info.defaultName, userId, token)
                    call.respond(newChat)
                }
            }

            // curl -X POST "http://127.0.0.1:9999/v1/me/signout/" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiI1Y2FlMWZlYy1jZTAzLTQ4YWEtOTIyYy1lYTA4MjU0MGY3NzIiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJwdXBraW4ifQ.MiueK0xGBsHR3dWFyPVo2W5FZ9x3zfikz_bfwFvp3vruMwY5Ots5AOnlwjHXGhzBsI9rpZB2U85_3xas3DzelQ"
            post("/v1/me/signout") {
                withAuthorizationParams { userId, token ->
                    call.respond(server.signOut(userId, token))
                }
            }

            // curl -v -X POST "http://127.0.0.1:9999/v1/chats/2/invite" --data '{ "userId": "ivanov" }' -H "Content-type: application/json" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiI1Y2FlMWZlYy1jZTAzLTQ4YWEtOTIyYy1lYTA4MjU0MGY3NzIiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJwdXBraW4ifQ.MiueK0xGBsHR3dWFyPVo2W5FZ9x3zfikz_bfwFvp3vruMwY5Ots5AOnlwjHXGhzBsI9rpZB2U85_3xas3DzelQ"
            post("/v1/chats/{id}/invite/") {
                withAuthorizationParams { userId, token ->
                    val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                    val info = call.receive<InviteChatInfo>()
                    server.usersInviteToChat(info.userId, chatId, userId, token)
                    call.respond(mapOf("status" to "OK"))
                }
            }

            // curl -v -X POST "http://127.0.0.1:9999/v1/chats/2/join" --data '{ "defaultName": "chat with Pupkin", "secret": "5cae1fec" }' -H "Content-type: application/json" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiJmNzRjNzNlMC01ZWZiLTQzYTctYmY0Mi0wMzBkNGJhNDY0ODMiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJpdmFub3YifQ.dlnzGan5DayBv_p-1C8Ko7juwiU5zFrossx1HBPtaWscwY51Vd0wMMvXOrtYFocijvnOSygo2MufPG6_rsapug"
            post("/v1/chats/{id}/join"){
                withAuthorizationParams { userId, token ->
                    val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                    val info = call.receive<JoinChatInfo>()
                    server.chatsJoin(chatId, info.secret, userId, token, info.defaultName)
                    call.respond(mapOf("status" to "OK"))
                }
            }

            // curl -v "http://127.0.0.1:9999/v1/me/chats" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiI1Y2FlMWZlYy1jZTAzLTQ4YWEtOTIyYy1lYTA4MjU0MGY3NzIiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJwdXBraW4ifQ.MiueK0xGBsHR3dWFyPVo2W5FZ9x3zfikz_bfwFvp3vruMwY5Ots5AOnlwjHXGhzBsI9rpZB2U85_3xas3DzelQ"
            // curl -v "http://127.0.0.1:9999/v1/me/chats" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiJmNzRjNzNlMC01ZWZiLTQzYTctYmY0Mi0wMzBkNGJhNDY0ODMiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJpdmFub3YifQ.dlnzGan5DayBv_p-1C8Ko7juwiU5zFrossx1HBPtaWscwY51Vd0wMMvXOrtYFocijvnOSygo2MufPG6_rsapug"
            get("/v1/me/chats") {
                withAuthorizationParams { userId, token ->
                    val chats = server.usersListChats(userId, token)
                    call.respond(chats)
                }
            }

            // curl -v "http://127.0.0.1:9999/v1/chats/2/members" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiI1Y2FlMWZlYy1jZTAzLTQ4YWEtOTIyYy1lYTA4MjU0MGY3NzIiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJwdXBraW4ifQ.MiueK0xGBsHR3dWFyPVo2W5FZ9x3zfikz_bfwFvp3vruMwY5Ots5AOnlwjHXGhzBsI9rpZB2U85_3xas3DzelQ"
            get("/v1/chats/{id}/members") {
                withAuthorizationParams { userId, token ->
                    val chatId = call.parameters["id"] ?.toInt() ?: throw ChatNotFoundException()
                    val members = server.chatsMembersList(chatId, userId, token)
                    call.respond(members)
                }
            }

            // curl -v -X POST "http://127.0.0.1:9999/v1/chats/2/messages/" --data '{ "text": "Всем чмоки в этом чате))))" }' -H "Content-type: application/json" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiJmNzRjNzNlMC01ZWZiLTQzYTctYmY0Mi0wMzBkNGJhNDY0ODMiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJpdmFub3YifQ.dlnzGan5DayBv_p-1C8Ko7juwiU5zFrossx1HBPtaWscwY51Vd0wMMvXOrtYFocijvnOSygo2MufPG6_rsapug"
            // curl -v -X POST "http://127.0.0.1:9999/v1/chats/2/messages/" --data '{ "text": "превед, медвед" }' -H "Content-type: application/json" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiI1Y2FlMWZlYy1jZTAzLTQ4YWEtOTIyYy1lYTA4MjU0MGY3NzIiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJwdXBraW4ifQ.MiueK0xGBsHR3dWFyPVo2W5FZ9x3zfikz_bfwFvp3vruMwY5Ots5AOnlwjHXGhzBsI9rpZB2U85_3xas3DzelQ"
            post("/v1/chats/{id}/messages/") {
                withAuthorizationParams { userId, token ->
                    val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                    val info = call.receive<NewMessageInfo>()
                    val newUser = server.chatMessagesCreate(chatId, info.text, userId, token)
                    call.respond(newUser)
                }
            }

            // curl -v -X GET "http://127.0.0.1:9999/v1/chats/1/messages/" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiJmNzRjNzNlMC01ZWZiLTQzYTctYmY0Mi0wMzBkNGJhNDY0ODMiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJpdmFub3YifQ.dlnzGan5DayBv_p-1C8Ko7juwiU5zFrossx1HBPtaWscwY51Vd0wMMvXOrtYFocijvnOSygo2MufPG6_rsapug"
            // curl -v -X GET "http://127.0.0.1:9999/v1/chats/2/messages/" -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiI1Y2FlMWZlYy1jZTAzLTQ4YWEtOTIyYy1lYTA4MjU0MGY3NzIiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJwdXBraW4ifQ.MiueK0xGBsHR3dWFyPVo2W5FZ9x3zfikz_bfwFvp3vruMwY5Ots5AOnlwjHXGhzBsI9rpZB2U85_3xas3DzelQ"
            get("/v1/chats/{id}/messages/") {
                withAuthorizationParams { userId, token ->
                    val chatId = call.parameters["id"] ?.toInt() ?: throw ChatNotFoundException()
                    val afterId = call.parameters["after_id"] ?.toInt() ?: 0
                    val messages = server.chatMessagesList(chatId, userId, token, afterId)
                    call.respond(messages)
                }
            }

            // curl -v "http://127.0.0.1:9999/v1/users/"  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiI1Y2FlMWZlYy1jZTAzLTQ4YWEtOTIyYy1lYTA4MjU0MGY3NzIiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJwdXBraW4ifQ.MiueK0xGBsHR3dWFyPVo2W5FZ9x3zfikz_bfwFvp3vruMwY5Ots5AOnlwjHXGhzBsI9rpZB2U85_3xas3DzelQ"
            get("/v1/users/") {
                withAuthorizationParams { userId, token ->
                    val namePattern = call.parameters["name"]
                    val users = server.usersListByName(namePattern,userId, token)
                    call.respond(users)
                }
            }

            // curl -v "http://127.0.0.1:9999/v1/users/pupkin"  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiJmNzRjNzNlMC01ZWZiLTQzYTctYmY0Mi0wMzBkNGJhNDY0ODMiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJpdmFub3YifQ.dlnzGan5DayBv_p-1C8Ko7juwiU5zFrossx1HBPtaWscwY51Vd0wMMvXOrtYFocijvnOSygo2MufPG6_rsapug"
            get("/v1/users/{id}") {
                withAuthorizationParams { userId, token ->
                    val userToFindId = call.parameters["id"] ?: throw UserNotFoundException()
                    call.respond(server.usersListById(userToFindId, userId, token).first())
                }
            }

            // curl -v "http://127.0.0.1:9999/v1/admin"  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsInNlc3Npb24iOiI1Y2FlMWZlYy1jZTAzLTQ4YWEtOTIyYy1lYTA4MjU0MGY3NzIiLCJpc3MiOiJrb3RsaW4tbWVzc2VuZ2VyIiwiaWQiOiJwdXBraW4ifQ.MiueK0xGBsHR3dWFyPVo2W5FZ9x3zfikz_bfwFvp3vruMwY5Ots5AOnlwjHXGhzBsI9rpZB2U85_3xas3DzelQ"
            get("/v1/admin") {
                withAuthorizationParams { _, _ ->
                    call.respond(server.getSystemUser())
                }
            }
        }
    }
}

suspend inline fun PipelineContext<Unit,ApplicationCall>.withAuthorizationParams(function: PipelineContext<Unit,ApplicationCall>.(userId: String, token:String) -> Unit) {
    val user = call.user
    val token = call.sessionToken
    if (user == null || token == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "_user_id and _token must be provided"))
        return
    }
    val userId = user.userId
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


