package ru.kotlin566.messenger.storage

import ru.kotlin566.messenger.server.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
/**
 * Хранилище пользователей, чатов, сообщений и пр.
 */




object Users : Table(){
    val userId: Column<String> = varchar("userId", 50).autoIncrement().primaryKey()
    val displayName: Column<String> = varchar("displayName", 50)
    val passwordHash: Column<String> = varchar("passwordHash", 64)
}


object Chats : Table(){
    val chatId: Column<Int> = integer("chatId").autoIncrement().primaryKey()
    val defaultName: Column<String> = varchar("defaultName", 64)
}

object Members : Table(){
    val memberId: Column<Int> = integer("memberId").autoIncrement().primaryKey()
    val chatId: Column<Int> = integer("chatId")
    val chatDisplayName: Column<String> = varchar("chatDisplayName", 64)
    val memberDisplayName: Column<String> = varchar("memberDisplayName", 64)
    val userId: Column<String> = varchar("userId", 64)
}

object  Messages : Table(){
    val messageId: Column<Int> = integer("messageId").autoIncrement().primaryKey()
    val memberId: Column<Int> = integer("memberId")
    val text: Column<String> = varchar("text", 1000)
    val createdOn: Column<Long> = long("createdOn")
}

class Storage {


    // FIXME: эффективнее было бы иметь структуры для быстрого поиска элементов по их id
    private val users =  mutableListOf<UserInfo>()
    private val chats = mutableListOf<ChatInfo>()
    private val members = mutableListOf<MemberInfo>()
    private val messages = mutableListOf<MessageInfo>()

    private val chatId2secret = mutableMapOf<Int, String>()
    private val token2userId = mutableMapOf<String, String>()

    fun clear() {
        transaction {
            token2userId.clear() //????
            SchemaUtils.drop(Chats)
            SchemaUtils.drop(Members)
            SchemaUtils.drop(Messages)
            SchemaUtils.create(Chats)
            SchemaUtils.create(Members)
            SchemaUtils.create(Messages)
        }
    }

    companion object {
        var nextChatId = 0
        var nextMemberId = 0
        var nextMessageId = 0
    }

    fun generateChatId(): Int {
        return nextChatId++
    }

    fun generateMemberId(): Int {
        return nextMemberId++
    }

    fun generateMessageId(): Int {
        return nextMessageId++
    }

    fun containsUser(userId: String): Boolean {
        return transaction { Users.select{Users.userId eq userId}.count()} > 0
        //return users.any { it.userId == userId }
    }

    fun addUser(newUserInfo: UserInfo) {
        if (containsUser(newUserInfo.userId)) {
            throw UserAlreadyExistsException()
        }
        transaction{
            Users.insert{
                it[displayName] = newUserInfo.displayName
                it[passwordHash] = newUserInfo.passwordHash
            }
        }
        //users.add(newUserInfo)
    }

    fun findUserById(userId: String): UserInfo? {
        var ans: UserInfo?
        ans = null
        transaction {
            Users.select { Users.userId eq userId }.forEach {
                ans = UserInfo(it[Users.userId], it[Users.displayName], it[Users.passwordHash])
            }
        }
        return ans
        //return users.first { it.userId == userId }
    }

    fun addToken(userId: String, token: String) {
        token2userId[token] = userId
    }

    fun getUserIdByToken(token: String) : String? {
        return token2userId[token]
    }

    fun removeToken(token: String) {
        token2userId.remove(token)
    }

    fun findUsersByPartOfName(partOfName: String?): List<UserInfo> {
        return transaction {
            Users.select { Users.displayName like "%$partOfName%" }.map {
                UserInfo(it[Users.userId], it[Users.displayName], it[Users.passwordHash])
            }
        }
        // return users.filter { partOfName == null || it.displayName.contains(partOfName) }
    }

    fun addChat(newChatInfo: ChatInfo) {
        transaction{
            Chats.insert{
                it[defaultName]= newChatInfo.defaultName
            }
        }
        //chats.add(newChatInfo)
    }

    fun containsChat(chatId: Int): Boolean {
        return transaction { Chats.select{Chats.chatId eq chatId}.count() } > 0
        // return chats.any { it.chatId == chatId }
    }

    fun findChatById(chatId: Int): ChatInfo? {
        var ans :ChatInfo?
        ans = null
        transaction {
            Chats.select { Chats.chatId eq chatId }.forEach {
                ans = ChatInfo(it[Chats.chatId], it[Chats.defaultName])
            }
        }
        return ans
        //return chats.firstOrNull { it.chatId == chatId }
    }

    fun containsMember(chatId: Int, userId: String) : Boolean {
        return transaction {Members.select{(Members.chatId eq chatId) and (Members.userId eq userId)}.count()} > 0
        // return members.any { it.chatId == chatId && it.userId == userId}
    }

    fun findMemberByChatIdAndUserId(chatId: Int, userId: String) : MemberInfo? {
        var ans : MemberInfo?
        ans = null
        transaction {
            Members.select { (Members.chatId eq chatId) and (Members.userId eq userId) }.forEach {
                ans = MemberInfo(it[Members.memberId],
                        it[Members.chatId],
                        it[Members.chatDisplayName],
                        it[Members.memberDisplayName],
                        it[Members.userId])
            }
        }
        return ans
        //return members.firstOrNull { it.chatId == chatId && it.userId == userId}
    }

    fun findMemberById(memberId: Int) : MemberInfo? {
        var ans :MemberInfo?
        ans = null
        transaction {
            Members.select { (Members.memberId eq memberId) }.forEach {
                ans = MemberInfo(it[Members.memberId],
                        it[Members.chatId],
                        it[Members.chatDisplayName],
                        it[Members.memberDisplayName],
                        it[Members.userId])
            }
        }
        return ans
        // return members.firstOrNull { it.memberId == memberId }
    }

    fun containsMember(memberId: Int) : Boolean {
        return transaction {Members.select{(Members.memberId eq memberId) }.count()} > 0
        // return members.any { it.memberId == memberId }
    }

    fun addChatMember(newMemberInfo: MemberInfo) {
        if (containsMember(newMemberInfo.chatId, newMemberInfo.userId)) {
            throw UserAlreadyMemberException()
        }
        transaction {
            Members.insert {
                it[memberId] = newMemberInfo.memberId
                it[chatDisplayName] = newMemberInfo.chatDisplayName
                it[memberDisplayName] = newMemberInfo.memberDisplayName
                it[chatId] = newMemberInfo.chatId
                it[userId] = newMemberInfo.userId
            }
        }
    }

    fun addChatSecret(chatId: Int, secret: String) {
        if (chatId2secret[chatId] != null) {
            throw SecretAlreadyExistsException()
        }
        chatId2secret[chatId] = secret
    }

    fun findChatIdsByUserId(userId: String) : List<Int> {
        return transaction {
            Members.select { Members.userId eq userId }.map {
                it[Members.chatId]
            }
        }
        //return members.filter { it.userId == userId }.map { it.chatId }
    }

    private fun findMemberIdsByChatId(chatId: Int) : List<Int> {
        return transaction {
            Members.select { Members.chatId eq chatId }.map {
                it[Members.memberId]
            }
        }
        //return findMembersByChatId(chatId).map { it.memberId }
    }

    fun findMembersByChatId(chatId: Int): List<MemberInfo> {
        return transaction {
            Members.select { Members.chatId eq chatId }.map {
                MemberInfo(it[Members.memberId],
                        it[Members.chatId],
                        it[Members.chatDisplayName],
                        it[Members.memberDisplayName],
                        it[Members.userId])
            }
        }
        // return members.filter { it.chatId == chatId }
    }

    fun findCommonChatIds(userId1: String, userId2: String): List<Int> {
        val chatIds = findChatIdsByUserId(userId1)
        return chatIds.filter { containsMember(it, userId2) }
    }

    fun getChatSecret(chatId: Int): String? {
        return chatId2secret[chatId]
    }

    fun addMessage(messageInfo: MessageInfo) {
        if (transaction {Messages.select { Messages.messageId eq messageInfo.messageId }.count()} > 0) {
            throw MessageAlreadyExistsException()
        }
        transaction {
            Messages.insert {
                it[messageId] = messageInfo.messageId
                it[createdOn] = messageInfo.createdOn.epochSecond // FIXME непонятно что делать с этим временем, как его настроить
                it[memberId] = messageInfo.memberId
                it[text] = messageInfo.text
            }
        }
        //messages.add(messageInfo)
    }
//
    fun findMessages(chatId: Int, afterMessageId : Int = 0) : List<MessageInfo> { //FIXME тоже непонятно что со временем
        val chatMembers = findMemberIdsByChatId(chatId)
        val createdAfter = if (afterMessageId > 0) {
            messages.firstOrNull { it.messageId == afterMessageId }?.createdOn
        }
        else {
            null
        }
        return messages
                .filter{ it.memberId in chatMembers && (createdAfter == null || it.createdOn >= createdAfter) }
                .sortedBy { it.createdOn }
    }
//
    fun findMessageById(messageId: Int) : MessageInfo? {
    var ans :MessageInfo?
    ans = null
    transaction {
        Messages.select { Messages.messageId eq messageId }.forEach {
            ans = MessageInfo(
                    it[Messages.messageId],
                    it[Messages.memberId],
                    it[Messages.text]
            )
        }
    }
        return ans
        //return messages.firstOrNull { it.messageId == messageId }
    }

    fun removeMessage(messageInfo: MessageInfo) {
        transaction {
            Messages.deleteWhere { Messages.messageId eq messageInfo.messageId }
        }
        //messages.remove(messageInfo)
    }

    fun removeMember(memberInfo: MemberInfo) {
        transaction {
            Members.deleteWhere { Members.memberId eq memberInfo.memberId }
        }
        //members.remove(memberInfo)
    }

}