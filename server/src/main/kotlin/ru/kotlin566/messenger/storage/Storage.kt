package ru.kotlin566.messenger.storage

import ch.qos.logback.core.subst.Token
import ru.kotlin566.messenger.server.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
/**
 * Хранилище пользователей, чатов, сообщений и пр.
 */


object Dbconst{
    const val nameLength = 64
    const val messageLength = 1000
}

object Users : Table(){
    val userId: Column<String> = varchar("userId", Dbconst.nameLength).autoIncrement().primaryKey()
    val displayName: Column<String> = varchar("displayName", Dbconst.nameLength)
    val passwordHash: Column<String> = varchar("passwordHash", Dbconst.nameLength)
}


object Chats : Table(){
    val chatId: Column<Int> = integer("chatId").autoIncrement().primaryKey()
    val defaultName: Column<String> = varchar("defaultName", Dbconst.nameLength)
}

object Members : Table(){
    val memberId: Column<Int> = integer("memberId").autoIncrement().primaryKey()
    val chatId: Column<Int> = integer("chatId")
    val chatDisplayName: Column<String> = varchar("chatDisplayName", Dbconst.nameLength)
    val memberDisplayName: Column<String> = varchar("memberDisplayName", Dbconst.nameLength)
    val userId: Column<String> = varchar("userId", Dbconst.nameLength)
}

object  Messages : Table(){
    val messageId: Column<Int> = integer("messageId").autoIncrement().primaryKey()
    val memberId: Column<Int> = integer("memberId")
    val text: Column<String> = varchar("text", Dbconst.messageLength)
    val createdOn: Column<Long> = long("createdOn")
}


object ChatId2secret : Table(){
    val chatId: Column<Int> = integer("chatId").primaryKey().uniqueIndex()
    val secret: Column <String> = varchar("secret", Dbconst.nameLength)
}

object Token2userId : Table(){
    val token: Column<String> = varchar("token", Dbconst.nameLength).primaryKey().uniqueIndex()
    val userId: Column <String> = varchar("userId", Dbconst.nameLength)
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
            //TODO специально ли мы не сбрасываем chatId2secret????
            SchemaUtils.drop(Token2userId)
            SchemaUtils.drop(Chats)
            SchemaUtils.drop(Members)
            SchemaUtils.drop(Messages)
            SchemaUtils.create(Token2userId)
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
    }

    fun addToken(userId: String, token: String) {
        transaction {
            Token2userId.insert{
                it[Token2userId.token] = token
                it[Token2userId.userId] = userId
            }
        }
    }

    fun getUserIdByToken(token: String) : String? {
        var ans: String? = null
        transaction {
            Token2userId.select{Token2userId.token eq token}.forEach {
                ans = it[Token2userId.userId]
            }
        }
        return ans
    }

    fun removeToken(token: String) {
        transaction {
            Token2userId.deleteWhere { Token2userId.token eq token }
        }
    }

    fun findUsersByPartOfName(partOfName: String?): List<UserInfo> {
        return transaction {
            Users.select { Users.displayName like "%$partOfName%" }.map {
                UserInfo(it[Users.userId], it[Users.displayName], it[Users.passwordHash])
            }
        }
    }

    fun addChat(newChatInfo: ChatInfo) {
        transaction{
            Chats.insert{
                it[defaultName]= newChatInfo.defaultName
            }
        }
    }

    fun containsChat(chatId: Int): Boolean {
        return transaction { Chats.select{Chats.chatId eq chatId}.count() } > 0
    }

    fun findChatById(chatId: Int): ChatInfo? {
        var ans :ChatInfo? = null
        transaction {
            Chats.select { Chats.chatId eq chatId }.forEach {
                ans = ChatInfo(it[Chats.chatId], it[Chats.defaultName])
            }
        }
        return ans
    }

    fun containsMember(chatId: Int, userId: String) : Boolean {
        return transaction {Members.select{(Members.chatId eq chatId) and (Members.userId eq userId)}.count()} > 0
    }

    fun findMemberByChatIdAndUserId(chatId: Int, userId: String) : MemberInfo? {
        var ans : MemberInfo? = null
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
    }

    fun findMemberById(memberId: Int) : MemberInfo? {
        var ans :MemberInfo? = null
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
    }

    fun containsMember(memberId: Int) : Boolean {
        return transaction {Members.select{(Members.memberId eq memberId) }.count()} > 0
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
        if (transaction { ChatId2secret.select{ChatId2secret.chatId eq chatId}.count() } > 0) {
            throw SecretAlreadyExistsException()
        }
        transaction {
            ChatId2secret.insert {
                it[ChatId2secret.chatId] = chatId
                it[ChatId2secret.secret] = secret
            }
        }
    }

    fun findChatIdsByUserId(userId: String) : List<Int> {
        return transaction {
            Members.select { Members.userId eq userId }.map {
                it[Members.chatId]
            }
        }
    }

    private fun findMemberIdsByChatId(chatId: Int) : List<Int> {
        return transaction {
            Members.select { Members.chatId eq chatId }.map {
                it[Members.memberId]
            }
        }
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
    }

    fun findCommonChatIds(userId1: String, userId2: String): List<Int> {
        val chatIds = findChatIdsByUserId(userId1)
        return chatIds.filter { containsMember(it, userId2) }
    }

    fun getChatSecret(chatId: Int): String? {
        var ans:String? = null
        transaction {
            ChatId2secret.select{ChatId2secret.chatId eq chatId}.forEach {
                ans = it[ChatId2secret.secret]
            }
        }
        return ans
    }

    fun addMessage(messageInfo: MessageInfo) {
        if (transaction {Messages.select { Messages.messageId eq messageInfo.messageId }.count()} > 0) {
            throw MessageAlreadyExistsException()
        }
        transaction {
            Messages.insert {
                it[messageId] = messageInfo.messageId
                it[createdOn] = messageInfo.createdOn
                it[memberId] = messageInfo.memberId
                it[text] = messageInfo.text
            }
        }
    }

    fun findMessages(chatId: Int, afterMessageId : Int = 0) : List<MessageInfo> {
        val chatMembers = findMemberIdsByChatId(chatId)
        val createdAfter:Long = if (afterMessageId > 0) {
            var ans: Long = -1
            transaction { Messages.select { Messages.messageId eq afterMessageId}.map{it[Messages.createdOn]} }.forEach {
                ans = it
            }
            ans
        }
        else {
            -1
        }
    return transaction { Messages.select{(Messages.memberId inList chatMembers) and (Messages.createdOn greaterEq createdAfter )}
            .orderBy(Messages.createdOn, SortOrder.ASC).map{
                MessageInfo(it[Messages.messageId],
                        it[Messages.memberId],
                        it[Messages.text],
                        it[Messages.createdOn]
                        )
            }
        }
    }

    fun findMessageById(messageId: Int) : MessageInfo? {
    var ans :MessageInfo? = null
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
    }

    fun removeMessage(messageInfo: MessageInfo) {
        transaction {
            Messages.deleteWhere { Messages.messageId eq messageInfo.messageId }
        }
    }

    fun removeMember(memberInfo: MemberInfo) {
        transaction {
            Members.deleteWhere { Members.memberId eq memberInfo.memberId }
        }
    }
}