package ru.kotlin566.messenger.server

import java.lang.Exception
import java.util.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import ru.kotlin566.messenger.storage.Storage
import java.lang.IllegalArgumentException
import java.security.SecureRandom

/**
 * Серверная часть мессенджера
 */
class MessengerServer {

    companion object {
        // TODO: storage must be internal!
        val storage = Storage()
        val systemUser = UserInfo("admin", "Administration", "")
        val passwordEncoder = BCryptPasswordEncoder(4, SecureRandom())
    }

    init {
        if (!storage.containsUser(systemUser.userId)) {
            storage.addUser(systemUser)
        }
    }

    fun usersCreate(userId: String, name: String, password: String) : UserInfo {
        if (storage.containsUser(userId)) {
            throw UserAlreadyExistsException()
        }
        val newUser = UserInfo(userId, name, passwordEncoder.encode(password))
        storage.addUser(newUser)
        createSystemChatForUser(newUser)
        return newUser
    }

    private fun createSystemChatForUser(userInfo: UserInfo) {
        val systemChat = doCreateChat("System for ${userInfo.userId}", systemUser)
        val member = MemberInfo(storage.generateMemberId(), systemChat.chatId, "System", userInfo.displayName, userInfo.userId)
        storage.addChatMember(member)
    }

    fun signIn(userId: String, password: String) : String {
        val user = getUserById(userId)
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw UserNotAuthorizedException()
        }
        val token = generateToken(userId)
        storage.addToken(userId, token)
        return token
    }

    fun signOut(userId: String, token: String) {
        checkUserAuthorization(userId, token)
        storage.removeToken(token)
    }

    fun usersListById(userIdToFind: String, userId: String, token: String) : List<UserInfo> {
        checkUserAuthorization(userId, token)
        val requestedUser = storage.findUserById(userIdToFind)
        if (requestedUser != null) {
            return listOf(requestedUser)
        }
        return emptyList()
    }

    fun usersListByName(namePattern : String? = null, userId: String, token: String) : List<UserInfo> {
        checkUserAuthorization(userId, token)
        return storage.findUsersByPartOfName(namePattern)
    }

    fun usersDelete(userId: String, token: String) {
        checkUserAuthorization(userId, token)

        TODO("not implemented")

        // FIXME: Что делать с данными, ссылающимися на пользователя?
        // Надо очень аккуратно удалить все его токены, чаты и сообщения
        // Проще ставить отметку о том, что пользователь удалён или иметь
        // специального "удалённого пользователя"
    }

    fun chatsCreate(chatName: String, userId: String, token: String) : ChatInfo {
        val user = checkUserAuthorization(userId, token)
        return doCreateChat(chatName, user)
    }

    private fun doCreateChat(chatName: String, userInfo: UserInfo): ChatInfo {
        val chatId = storage.generateChatId()
        val chat = ChatInfo(chatId, chatName)
        storage.addChat(chat)
        storage.addChatSecret(chatId, generateChatSecret(chatId))
        val member = MemberInfo(storage.generateMemberId(), chatId, chatName, userInfo.displayName, userInfo.userId)
        storage.addChatMember(member)
        return chat
    }

    // FIXME: debug secrets for first chats
    private fun generateChatSecret(chatId: Int) =
        when (chatId) {
            2 -> "5cae1fec"
            3 -> "f74c73e0"
            else -> UUID.randomUUID().toString().substring(0..7)
        }

    // FIXME: debug token for pupkin and invanov
    private fun generateToken(userId: String) =
            when (userId) {
                "pupkin" -> "5cae1fec-ce03-48aa-922c-ea082540f772"
                "ivanov" -> "f74c73e0-5efb-43a7-bf42-030d4ba46483"
                else -> UUID.randomUUID().toString()
            }

    fun usersInviteToChat(userIdToInvite: String, chatId: Int, userId: String, token: String) {
        val user = checkUserAuthorization(userId, token)

        // Проверяем, что пользователь сам является участником чата
        checkUserIsMemberOfChat(chatId, user)

        val secret = storage.getChatSecret(chatId) ?: throw ChatNotFoundException()
        // Отправляем приглашение в виде системного сообщения, содержащего chatId и secret
        val text = "Пользователь ${user.displayName} (${user.userId}) приглашает вас в чат $chatId. Используйте пароль '$secret'"
        createSystemMessage(userIdToInvite, text)

    }

    private fun createSystemMessage(userIdToInvite: String, text: String) {
        val systemChat = getSystemChatId(userIdToInvite)
        val systemMember = storage.findMemberByChatIdAndUserId(systemChat, systemUser.userId)
                ?: throw InternalError("System user is not member of system chat?")
        doMessagesCreate(systemMember.memberId, text)
    }

    fun chatsJoin(chatId: Int, secret: String, userId: String, token: String, chatName: String? = null) {
        val user = checkUserAuthorization(userId, token)
        if (storage.findMemberByChatIdAndUserId(chatId, user.userId) != null) {
            throw UserAlreadyMemberException()
        }
        val defaultChatName = storage.findChatById(chatId)?.defaultName ?: throw ChatNotFoundException()
        val realSecret = storage.getChatSecret(chatId) ?: throw ChatNotFoundException()
        if (realSecret != secret) {
            throw WrongChatSecretException()
        }
        val member = MemberInfo(storage.generateMemberId(), chatId, chatName
                ?: defaultChatName, user.displayName, user.userId)
        storage.addChatMember(member)
    }

    fun chatsLeave(chatId: Int, userId: String, token: String) {
        val user = checkUserAuthorization(userId, token)
        val member = checkUserIsMemberOfChat(chatId, user)
        // FIXME: Что будет с сообщениями от этого участника? - они просто пропадут
        // Лучше ввести флаг, показывающий является ли участник активным. Неактивный участник не видит чат у себя в списке чатов,
        // но при этом остальные участники продолжают видеть его старые сообщения
        storage.removeMember(member)
    }

    fun usersListChats(userId: String, token: String) : List<ChatInfo> {
        val user = checkUserAuthorization(userId, token)
        val chatIds = storage.findChatIdsByUserId(user.userId)
        val result = mutableListOf<ChatInfo>()
        chatIds.forEach {
            val chat = storage.findChatById(it)
            if (chat != null) {
                result.add(chat)
            }
        }
        return result
    }

    fun chatsMembersList(chatId: Int, userId: String, token: String) : List<MemberInfo> {
        val user = checkUserAuthorization(userId, token)
        checkUserIsMemberOfChat(chatId, user)
        return storage.findMembersByChatId(chatId)
    }

    fun chatMessagesCreate(chatId: Int, text: String, userId: String, token: String) : MessageInfo {
        val user = checkUserAuthorization(userId, token)
        val member = checkUserIsMemberOfChat(chatId, user)
        return doMessagesCreate(member.memberId, text)
    }

    private fun doMessagesCreate(memberId: Int, text: String): MessageInfo {
        val messageId = storage.generateMessageId()
        val message = MessageInfo(messageId, memberId, text)
        storage.addMessage(message)
        return message
    }

    fun chatMessagesList(chatId: Int, userId: String, token: String, afterId: Int = 1) : List<MessageInfo> {
        if (afterId < 0) {
            throw IllegalArgumentException("afterId parameters must be > 0")
        }
        val user = checkUserAuthorization(userId, token)
        checkUserIsMemberOfChat(chatId, user)
        return storage.findMessages(chatId, afterId)
    }

    fun chatMessagesDeleteById(messageId: Int, userId: String, token: String) {
        val user = checkUserAuthorization(userId, token)
        val message = storage.findMessageById(messageId)
        if (message != null) {
            val member = storage.findMemberById(message.memberId) ?: throw ChatNotFoundException()
            checkUserIsMemberOfChat(member.chatId, user)
            // FIXME: Что делать, если мы хотим показывать заглушки на месте удалённых сообщений?
            // Лучше удалять текст и помечать сообщение как удалённое
            storage.removeMessage(message)
        }
    }

    private fun checkUserIsMemberOfChat(chatId: Int, userInfo: UserInfo) =
            storage.findMemberByChatIdAndUserId(chatId, userInfo.userId) ?: throw UserNotMemberException()

    private fun getSystemChatId(userId: String) = storage.findCommonChatIds(userId, systemUser.userId).first()

    private fun getUserById(userId: String) = storage.findUserById(userId) ?: throw UserNotFoundException()

    fun checkUserAuthorization(userId: String, token: String) : UserInfo {
        val user = getUserById(userId)
        val userIdByToken = storage.getUserIdByToken(token) ?: throw UserNotAuthorizedException()
        if (user.userId != userIdByToken) throw UserNotAuthorizedException()
        return user
    }

    fun getSystemUserId(): String {
        return systemUser.userId
    }

    fun getSystemUser(): UserInfo {
        return systemUser
    }
}

class UserNotMemberException : Exception()
class UserAlreadyMemberException : Exception()
class MessageAlreadyExistsException : Exception()
class ChatNotFoundException : Exception()
class WrongChatSecretException : Exception()
class SecretAlreadyExistsException : Exception()
class UserNotFoundException : Exception()
class UserNotAuthorizedException : Exception()
class UserAlreadyExistsException : Exception()
