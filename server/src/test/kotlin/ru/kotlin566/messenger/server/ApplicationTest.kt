package ru.kotlin566.messenger.server

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.util.NoSuchElementException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

// TODO cleanup packages' mess

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationTest {
    private val myServer = MessengerServer()
    private val dummyCounter = AtomicInteger(0)

    internal fun newDummy(displayName: String = "Teapot", password: String = "i_am_a_teapot") : NewUserInfo {
        val idx = dummyCounter.getAndIncrement()
        return NewUserInfo("teapot$idx", displayName, password)
    }

    internal fun getToken(user: NewUserInfo) : String {
        try { myServer.usersCreate(user.userId, user.displayName, user.password) }
        catch (e: UserAlreadyExistsException) { }
        val token = myServer.signIn(user.userId, user.password)
        assertNotNull(token)
        return token
    }

    internal fun getUserInfo(user: NewUserInfo) : UserInfo {
        val token = getToken(user)
        return myServer.usersListById(user.userId, user.userId, token)[0]
    }

    internal fun createChat(user: NewUserInfo, token: String, name: String = "Tea Party") : ChatInfo {
        val chatId = myServer.chatsCreate(name, user.userId, token)
        assertNotNull(chatId)
        val listOfChats = myServer.usersListChats(user.userId, token)
        assertSame(chatId, listOfChats.find { it == chatId })
        return chatId
    }

    internal fun getSystemMessageAboutChat(user: NewUserInfo, token: String, chatId: ChatInfo) : List<MessageInfo>
    {
        // Find chatId with system
        val systemId = myServer.getSystemUserId()
        val chatIdWithSystem = myServer.usersListChats(user.userId, token).find {
            val adminMember = myServer.chatsMembersList(it.chatId, user.userId, token).find {
                it.userId == systemId
            }
            return@find adminMember != null
        }
        assertNotNull(chatIdWithSystem)

        // Check if message from system with invite exists
        return myServer.chatMessagesList(chatIdWithSystem!!.chatId, user.userId, token).filter {
            it.text.contains(chatId.chatId.toString())
        }
    }
    internal fun getSecret(message: MessageInfo) : String {
        val messageText = message.text
        // Get secret
        val idx2 = messageText.lastIndexOf('\'')
        val idx1 = messageText.lastIndexOf('\'', idx2 - 1)
        return messageText.subSequence(idx1 + 1, idx2).toString()
    }

    internal fun getSecret(user: NewUserInfo, token: String, chatId: ChatInfo) : String {
        val message = getSystemMessageAboutChat(user, token, chatId)[0]
        return getSecret(message)
    }

    @Nested
    inner class UserCreationTest {
        @TestFactory
        fun testNormal() : Collection<DynamicTest> {
            val tests = listOf(
                    Pair(newDummy(), "normal"),
                    Pair(NewUserInfo("Powerلُلُصّبُلُلصّبُررً ॣ ॣh ॣ ॣ冗\n" +
                            "\uD83C\uDFF30\uD83C\uDF08️\n" +
                            "జ్ఞ\u200Cా", "\u202A\u202Atest\u202A\n" +
                            "\u202Btest\u202B\n" +
                            "\u2029test\u2029\n" +
                            "test\u2060test\u202B\n" +
                            "\u2066test\u2067", "Ṱ̺̺̕o͞ ̷i̲̬͇̪͙n̝̗͕v̟̜̘̦͟o̶̙̰̠kè͚̮̺̪̹̱̤ ̖t̝͕̳̣̻̪͞h̼͓̲̦̳̘̲e͇̣̰̦̬͎ ̢̼̻̱̘h͚͎͙̜̣̲ͅi̦̲̣̰̤v̻͍e̺̭̳̪̰-m̢iͅn̖̺̞̲̯̰d̵̼̟͙̩̼̘̳ ̞̥̱̳̭r̛̗̘e͙p͠r̼̞̻̭̗e̺̠̣͟s̘͇̳͍̝͉e͉̥̯̞̲͚̬͜ǹ̬͎͎̟̖͇̤t͍̬̤͓̼̭͘ͅi̪̱n͠g̴͉ ͏͉ͅc̬̟h͡a̫̻̯͘o̫̟̖͍̙̝͉s̗̦̲.̨̹͈̣\n" +
                            "̡͓̞ͅI̗̘̦͝n͇͇͙v̮̫ok̲̫̙͈i̖͙̭̹̠̞n̡̻̮̣̺g̲͈͙̭͙̬͎ ̰t͔̦h̞̲e̢̤ ͍̬̲͖f̴̘͕̣è͖ẹ̥̩l͖͔͚i͓͚̦͠n͖͍̗͓̳̮g͍ ̨o͚̪͡f̘̣̬ ̖̘͖̟͙̮c҉͔̫͖͓͇͖ͅh̵̤̣͚͔á̗̼͕ͅo̼̣̥s̱͈̺̖̦̻͢.̛̖̞̠̫̰\n" +
                            "̗̺͖̹̯͓Ṯ̤͍̥͇͈h̲́e͏͓̼̗̙̼̣͔ ͇̜̱̠͓͍ͅN͕͠e̗̱z̘̝̜̺͙p̤̺̹͍̯͚e̠̻̠͜r̨̤͍̺̖͔̖̖d̠̟̭̬̝͟i̦͖̩͓͔̤a̠̗̬͉̙n͚͜ ̻̞̰͚ͅh̵͉i̳̞v̢͇ḙ͎͟-҉̭̩̼͔m̤̭̫i͕͇̝̦n̗͙ḍ̟ ̯̲͕͞ǫ̟̯̰̲͙̻̝f ̪̰̰̗̖̭̘͘c̦͍̲̞͍̩̙ḥ͚a̮͎̟̙͜ơ̩̹͎s̤.̝̝ ҉Z̡̖̜͖̰̣͉̜a͖̰͙̬͡l̲̫̳͍̩g̡̟̼̱͚̞̬ͅo̗͜.̟\n" +
                            "̦H̬̤̗̤͝e͜ ̜̥̝̻͍̟́w̕h̖̯͓o̝͙̖͎̱̮ ҉̺̙̞̟͈W̷̼̭a̺̪͍į͈͕̭͙̯̜t̶̼̮s̘͙͖̕ ̠̫̠B̻͍͙͉̳ͅe̵h̵̬͇̫͙i̹͓̳̳̮͎̫̕n͟d̴̪̜̖ ̰͉̩͇͙̲͞ͅT͖̼͓̪͢h͏͓̮̻e̬̝̟ͅ ̤̹̝W͙̞̝͔͇͝ͅa͏͓͔̹̼̣l̴͔̰̤̟͔ḽ̫.͕\n" +
                            "Z̮̞̠͙͔ͅḀ̗̞͈̻̗Ḷ͙͎̯̹̞͓G̻O̭̗̮"),
                            "big and scary")
            )

            return tests.map {
                dynamicTest("User is ${it.second}.") {
                    val testUser = it.first
                    val userInfo = myServer.usersCreate(testUser.userId, testUser.displayName, testUser.password)
                    assertEquals(testUser.displayName, userInfo.displayName)
                    assertEquals(testUser.userId, userInfo.userId)
                }
            }.toList()
        }

        @TestFactory
        fun testExisting() : Collection<DynamicTest> {
            val tests = listOf(
                    Triple(NewUserInfo("teapotM1", "Teapot", "i_am_a_teapot"),
                            NewUserInfo("teapotM1", "Teapot", "i_am_a_teapot"),
                            "are identical"),
                    Triple(NewUserInfo("teapotM2", "Teapot", "i_am_a_teapot"),
                            NewUserInfo("teapotM2", "CoffeePot", "i_am_a_teapot"),
                            "have same ids")
            )

            return tests.map {
                dynamicTest("Existing users test, users ${it.third}.") {
                    val testUser = it.first
                    val testUser2 = it.second
                    myServer.usersCreate(testUser.userId, testUser.displayName, testUser.password)
                    assertThrows(UserAlreadyExistsException::class.java) {myServer.usersCreate(testUser2.userId, testUser2.displayName, testUser2.password)}
                }
            }.toList()
        }
    }

    @Nested
    inner class UserLoginLogoutTest {
        @Test
        fun testNormal() {
            val user = newDummy()
            val userInfo = getUserInfo(user)
            assertEquals(user.displayName, userInfo.displayName)
            assertEquals(user.userId, userInfo.userId)
            val token = getToken(user)
            assertNotNull(token)
            val gotUserInfo = myServer.checkUserAuthorization(user.userId, token)
            assertSame(userInfo, gotUserInfo)
            myServer.signOut(user.userId, token)
            assertThrows(UserNotAuthorizedException::class.java) {myServer.checkUserAuthorization(user.userId, token)}
        }

        @Test
        fun testBadPassword() {
            val user = newDummy()
            val userInfo = getUserInfo(user)
            assertEquals(user.displayName, userInfo.displayName)
            assertEquals(user.userId, userInfo.userId)
            assertThrows(UserNotAuthorizedException::class.java) {myServer.signIn(user.userId, user.password + "_and_i_like_tea")}
        }

        @Test
        fun testBadLogout() {
            val user = newDummy()
            val userInfo = getUserInfo(user)
            assertEquals(user.displayName, userInfo.displayName)
            assertEquals(user.userId, userInfo.userId)
            val token = getToken(user)
            assertNotNull(token)
            val gotUserInfo = myServer.checkUserAuthorization(user.userId, token)
            assertSame(userInfo, gotUserInfo)
            assertThrows(UserNotAuthorizedException::class.java) {myServer.signOut(user.userId, token + "qwerty")}
        }

        @Test
        fun testNonExistingUserLogin() {
            val user = newDummy()
            val userInfo = getUserInfo(user)
            assertEquals(user.displayName, userInfo.displayName)
            assertEquals(user.userId, userInfo.userId)
            assertThrows(NoSuchElementException::class.java) {myServer.signIn(user.userId + "_qwerty鷗", user.password)}
        }

        @Test
        fun testNonExistingLogout() {
            val user = newDummy()
            val userInfo = getUserInfo(user)
            assertEquals(user.displayName, userInfo.displayName)
            assertEquals(user.userId, userInfo.userId)
            val token = getToken(user)
            assertNotNull(token)
            val gotUserInfo = myServer.checkUserAuthorization(user.userId, token)
            assertSame(userInfo, gotUserInfo)
            assertThrows(NoSuchElementException::class.java) {myServer.signOut(user.userId + "_1qwerty鷗", token)}
        }
    }

    @Nested
    inner class UsersListByIdTest {
        @Test
        fun testNormalYourself() {
            val user = newDummy()
            val userInfo = getUserInfo(user)
            val token = getToken(user)
            assertNotNull(token)
            val list = myServer.usersListById(user.userId, user.userId, token)
            assertEquals(1, list.size)
            assertSame(userInfo, list[0])
        }

        @Test
        fun testNormalAnother() {
            val myUser = newDummy()
            val token = getToken(myUser)

            val user = newDummy()
            val userInfo = getUserInfo(user)

            assertNotNull(token)
            val list = myServer.usersListById(user.userId, myUser.userId, token)
            assertEquals(1, list.size)
            assertSame(userInfo, list[0])
        }

        @Test
        fun testNonExistingAnother() {
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            assertThrows(NoSuchElementException::class.java) {myServer.usersListById(user.userId + "_qwerty鷗", user.userId, token)}
        }

        @Test
        fun testNonExistingYou() {
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            assertThrows(NoSuchElementException::class.java) {myServer.usersListById(user.userId, user.userId + "_qwerty鷗", token)}
        }

        @Test
        fun testWrongToken() {
            val user = newDummy()
            getUserInfo(user)
            val token = getToken(user)
            assertNotNull(token)
            assertThrows(UserNotAuthorizedException::class.java) {myServer.usersListById(user.userId, user.userId, token + "qwerty")}
        }
    }

    @Nested
    inner class UsersListByNameTest {
        @Test
        fun testNormal() {
            val user = newDummy(displayName = "NotATeapot_NONONO")
            val userInfo = getUserInfo(user)
            val token = getToken(user)
            assertNotNull(token)
            val list = myServer.usersListByName(user.displayName, user.userId, token)
            assertEquals(1, list.size)
            assertSame(userInfo, list[0])

            val testUser2 = newDummy(user.displayName + "2")
            val userInfo2 = getUserInfo(testUser2)
            val list2 = myServer.usersListByName(user.displayName, user.userId, token)
            assertEquals(2, list2.size)
            assertSame(userInfo2, list2.find { it.userId == testUser2.userId })
            assertSame(userInfo, list2.find { it.userId == user.userId })
        }

        @Test
        fun testNonExistingAnother() {
            val myUser = newDummy()
            val token = getToken(myUser)
            assertNotNull(token)
            val list = myServer.usersListByName(myUser.displayName + "_qwerty鷗", myUser.userId, token)
            assertEquals(0, list.size)
        }

        @Test
        fun testNonExistingYou() {
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            assertThrows(NoSuchElementException::class.java) {myServer.usersListById(user.displayName, user.userId + "_qwerty鷗", token)}
        }

        @Test
        fun testWrongToken() {
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            assertThrows(UserNotAuthorizedException::class.java) {myServer.usersListById(user.displayName, user.userId, token + "qwerty")}
        }
    }

    // TODO [not implemented] testDeleteUser(): delete user, delete non-existing user, delete with using wrong auth.

    @Nested
    inner class ChatsCreateTest {
        @Test
        fun testNormal() {
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            val chatId = createChat(user, token)
            assertNotNull(chatId)
            val listOfChats = myServer.usersListChats(user.userId, token)
            assertSame(chatId, listOfChats.find { it == chatId })
        }

        @Test
        fun testNonExisting() {
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            assertThrows(NoSuchElementException::class.java) {myServer.chatsCreate("Tea Party", user.userId + "_qwerty鷗", token)}
        }

        @Test
        fun testWrongToken() {
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            assertThrows(UserNotAuthorizedException::class.java) {createChat(user, token + "qwerty")}
        }
    }

    @Nested
    inner class UsersInviteToChatTest {
        @Test
        fun testNormal() {
            // Create init user and chat
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)

            val chatId = createChat(user, token)
            assertNotNull(chatId)
            val listOfChats = myServer.usersListChats(user.userId, token)
            assertSame(chatId, listOfChats.find { it == chatId })

            // Create user, who will be invited
            val invUser = newDummy()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)

            val messages = getSystemMessageAboutChat(invUser, invToken, chatId)
            assertNotNull(messages)
        }

        @Test
        fun testNormalAlreadyInvited() {
            // Create init user and chat
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            val chatId = createChat(user, token)
            assertNotNull(chatId)
            val listOfChats = myServer.usersListChats(user.userId, token)
            assertSame(chatId, listOfChats.find { it == chatId })

            // Create user, who will be invited
            val invUser = newDummy()
            val invToken = getToken(invUser)

            // Invite twice
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)

            val messages = getSystemMessageAboutChat(invUser, invToken, chatId)
            assertEquals(2, messages.size)
            assertEquals(messages[0].text, messages[1].text)
        }

        @Test
        fun testNonExistingInvite() {
            // Create init user and chat
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            val chatId = createChat(user, token)
            assertNotNull(chatId)

            // Invite non-existing
            assertThrows(NoSuchElementException::class.java) {myServer.usersInviteToChat(user.userId + "_qwerty鷗", chatId.chatId, user.userId, token)}
        }

        @Test
        fun testNonExistingChat() {
            // Create init user and chat
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            val chatId = createChat(user, token)
            assertNotNull(chatId)

            val invUser = newDummy()
            getUserInfo(invUser)
            // Invite non-existing
            assertThrows(UserNotMemberException::class.java) {myServer.usersInviteToChat(invUser.userId, chatId.chatId - 30239566, user.userId, token)}
        }

        @Test
        fun testWrongToken() {
            // Create init user and chat
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            val chatId = createChat(user, token)
            assertNotNull(chatId)

            val invUser = newDummy()
            getUserInfo(invUser)

            // Invite non-existing
            assertThrows(UserNotAuthorizedException::class.java) {myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token + "_qwerty鷗")}
        }
    }

    // TODO testChatsJoin: invite normal person to existing chat, invite already joined person to a chat,
    // TODO invite normal person to non-existing chat, invite non-existing person to chat, invite normal person to chat with wrong secret,
    // TODO invite normal person with wrong auth to chat, invite non-existing person to non-existing chat, invite normal person with wrong auth to chat with wrong secret.

    @Nested
    inner class ChatsJoinTest {
        @Test
        fun testNormal() {
            // Create init user and chat
            val user = newDummy()
            val token = getToken(user)
            assertNotNull(token)
            val chatId = createChat(user, token)
            assertNotNull(chatId)
            val listOfChats = myServer.usersListChats(user.userId, token)
            assertSame(chatId, listOfChats.find { it == chatId })

            // Create user, who will be invited
            val invUser = newDummy()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)

            myServer.chatsJoin(chatId.chatId, secret.toString(), invUser.userId, invToken)
            assert(myServer.usersListChats(invUser.userId, invToken).contains(chatId))
        }
    }

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

    // TODO testGetSystemUserId & testGetSystemUser: normal.
}