package ru.kotlin566.messenger.server


import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.NoSuchElementException
import kotlin.test.assertFails

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationTest {
    private val myServer = MessengerServer()
    private val dummyCounter = AtomicInteger(0)
    private val baseUserName = UUID.randomUUID().toString()

    internal fun newDummyUser(displayName: String = "Teapot", password: String = "i_am_a_teapot",
                              createOnServer: Boolean = true): NewUserInfo {
        val idx = dummyCounter.getAndIncrement()
        val user = NewUserInfo(baseUserName + "_teapot$idx", displayName, password)
        if(createOnServer)
            myServer.usersCreate(user.userId, user.displayName, user.password)
        return user
    }

    internal fun getToken(user: NewUserInfo): String {
        val token = myServer.signIn(user.userId, user.password)
        assertNotNull(token)
        return token
    }

    internal fun getUserInfo(user: NewUserInfo): UserInfo {
        val token = getToken(user)
        return myServer.usersListById(user.userId, user.userId, token).first()
    }

    internal fun createChat(user: NewUserInfo, token: String, name: String = "Tea Party"): ChatInfo {
        val chatId = myServer.chatsCreate(name, user.userId, token)
        assertNotNull(chatId)
        val listOfChats = myServer.usersListChats(user.userId, token)
        assertEquals(chatId, listOfChats.find { it == chatId })
        return chatId
    }

    internal fun getSystemMessageAboutChat(user: NewUserInfo, token: String, chatId: ChatInfo): List<MessageInfo> {
        // Find chatId with system
        val systemId = myServer.getSystemUserId()
        val chatIdWithSystem = myServer.usersListChats(user.userId, token).first {
            myServer.chatsMembersList(it.chatId, user.userId, token).any { memberInfo ->
                memberInfo.userId == systemId
            }
        }
        assertNotNull(chatIdWithSystem)

        // Check if message from system with invite exists
        val messages = myServer.chatMessagesList(chatIdWithSystem.chatId, user.userId, token).filter {
            it.text.contains(chatId.chatId.toString())
        }
        assertNotNull(messages)
        return messages
    }

    internal fun getSecret(message: MessageInfo): String {
        val messageText = message.text
        // Get secret
        val idx2 = messageText.lastIndexOf('\'')
        val idx1 = messageText.lastIndexOf('\'', idx2 - 1)
        val secret = messageText.subSequence(idx1 + 1, idx2).toString()
        assertNotNull(secret)
        return secret
    }

    internal fun getSecret(user: NewUserInfo, token: String, chatId: ChatInfo): String {
        val message = getSystemMessageAboutChat(user, token, chatId).last()
        return getSecret(message)
    }

    @Nested
    inner class UserCreationTest {
        @TestFactory
        fun testNormal(): Collection<DynamicTest> {
            val tests = listOf(
                    Pair(newDummyUser(createOnServer = false), "normal"),
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
        fun testExisting(): Collection<DynamicTest> {
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
                    assertThrows(UserAlreadyExistsException::class.java) {
                        myServer.usersCreate(testUser2.userId, testUser2.displayName, testUser2.password) }
                }
            }.toList()
        }
    }

    @Nested
    inner class UserLoginLogoutTest {
        @Test
        fun testNormal() {
            val user = newDummyUser()
            val userInfo = getUserInfo(user)
            assertEquals(user.displayName, userInfo.displayName)
            assertEquals(user.userId, userInfo.userId)
            val token = getToken(user)
            val gotUserInfo = myServer.checkUserAuthorization(user.userId, token)
            assertEquals(userInfo, gotUserInfo) //TODO как-то странно сработал assertSame- действительно нужно именно его использовать(про одинаковых юзеров говорит, что они разные)
            myServer.signOut(user.userId, token)
            assertThrows(UserNotAuthorizedException::class.java) { myServer.checkUserAuthorization(user.userId, token) }
        }

        @Test
        fun testBadPassword() {
            val user = newDummyUser()
            getUserInfo(user)
            assertThrows(UserNotAuthorizedException::class.java) { myServer.signIn(user.userId, user.password + "_and_i_like_tea") }
        }

        @Test
        fun testBadLogout() {
            val user = newDummyUser()
            val userInfo = getUserInfo(user)
            val token = getToken(user)
            val gotUserInfo = myServer.checkUserAuthorization(user.userId, token)
            assertEquals(userInfo, gotUserInfo)
            assertThrows(UserNotAuthorizedException::class.java) { myServer.signOut(user.userId, token + "qwerty") }
        }

        @Test
        fun testNonExistingUserLogin() {
            val user = newDummyUser()
            getUserInfo(user)
            assertThrows(UserNotFoundException::class.java) { myServer.signIn(user.userId + "_qwerty鷗", user.password) }
        }

        @Test
        fun testNonExistingLogout() {
            val user = newDummyUser()
            val userInfo = getUserInfo(user)
            val token = getToken(user)
            val gotUserInfo = myServer.checkUserAuthorization(user.userId, token)
            assertEquals(userInfo, gotUserInfo)
            assertThrows(UserNotFoundException::class.java) { myServer.signOut(user.userId + "_1qwerty鷗", token) }
        }
    }

    @Nested
    inner class UsersListByIdTest {
        @Test
        fun testNormalYourself() {
            val user = newDummyUser()
            val userInfo = getUserInfo(user)
            val token = getToken(user)
            val list = myServer.usersListById(user.userId, user.userId, token)
            assertEquals(1, list.size)
            assertEquals(userInfo, list[0])
        }

        @Test
        fun testNormalAnother() {
            val myUser = newDummyUser()
            val token = getToken(myUser)

            val user = newDummyUser()
            val userInfo = getUserInfo(user)

            val list = myServer.usersListById(user.userId, myUser.userId, token)
            assertEquals(1, list.size)
            assertEquals(userInfo, list[0])
        }

        @Test
        fun testNonExistingAnother() {
            val user = newDummyUser()
            val token = getToken(user)
            //TODO если ничего не найдется, то будет просто пустой лист, никакого исключения быть не должно
            //assertThrows(UserNotFoundException::class.java) { myServer.usersListById(user.userId + "_qwerty鷗", user.userId, token) }
            val checkSize = myServer.usersListById(user.userId + "_qwerty鷗", user.userId, token).size
            assertEquals(0, checkSize)
        }

        @Test
        fun testNonExistingYou() {
            val user = newDummyUser()
            val token = getToken(user)
            assertThrows(UserNotFoundException::class.java) { myServer.usersListById(user.userId, user.userId + "_qwerty鷗", token) }
        }

        @Test
        fun testWrongToken() {
            val user = newDummyUser()
            getUserInfo(user)
            val token = getToken(user)
            assertThrows(UserNotAuthorizedException::class.java) { myServer.usersListById(user.userId, user.userId, token + "qwerty") }
        }
    }

    @Nested
    inner class UsersListByNameTest {
        @Test
        fun testNormal() {
            val user = newDummyUser(displayName = "NotATeapot_OHNO")
            val userInfo = getUserInfo(user)
            val token = getToken(user)

            val list = myServer.usersListByName(user.displayName, user.userId, token)
            assertEquals(1, list.size)
            assertEquals(userInfo, list[0])

            val testUser2 = newDummyUser(user.displayName + "2")
            val userInfo2 = getUserInfo(testUser2)
            val list2 = myServer.usersListByName(user.displayName, user.userId, getToken(user))
            assertEquals(2, list2.size)
            assertEquals(userInfo2, list2.find { it.userId == testUser2.userId })
            assertEquals(userInfo, list2.find { it.userId == user.userId })
        }

        @Test
        fun testNonExistingAnother() {
            val myUser = newDummyUser()
            val token = getToken(myUser)
            val list = myServer.usersListByName(myUser.displayName + "_qwerty鷗", myUser.userId, token)
            assertEquals(0, list.size)
        }

        @Test
        fun testNonExistingYou() {
            val user = newDummyUser()
            val token = getToken(user)
            assertThrows(UserNotFoundException::class.java) { myServer.usersListById(user.displayName, user.userId + "_qwerty鷗", token) }
        }

        @Test
        fun testWrongToken() {
            val user = newDummyUser()
            val token = getToken(user)
            assertThrows(UserNotAuthorizedException::class.java) { myServer.usersListById(user.displayName, user.userId, token + "qwerty") }
        }
    }

    // TODO [not implemented] testDeleteUser(): delete user, delete non-existing user, delete with using wrong auth.

    @Nested
    inner class ChatsCreateTest {
        @Test
        fun testNormal() {
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)
            val listOfChats = myServer.usersListChats(user.userId, getToken(user))
            assertEquals(chatId, listOfChats.find { it == chatId })
        }

        @Test
        fun testNonExisting() {
            val user = newDummyUser()
            val token = getToken(user)
            assertThrows(UserNotFoundException::class.java) { myServer.chatsCreate("Tea Party", user.userId + "_qwerty鷗", token) }
        }

        @Test
        fun testWrongToken() {
            val user = newDummyUser()
            val token = getToken(user)
            assertThrows(UserNotAuthorizedException::class.java) { createChat(user, token + "qwerty") }
        }
    }

    @Nested
    inner class UsersInviteToChatTest {
        @Test
        fun testNormal() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)

            val chatId = createChat(user, token)
            val listOfChats = myServer.usersListChats(user.userId, token)
            assertEquals(chatId, listOfChats.find { it == chatId })

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, getToken(user))

            val messages = getSystemMessageAboutChat(invUser, invToken, chatId)
            getSecret(messages.last())
        }

        @Test
        fun testNonExistingInvite() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            // Invite non-existing
            assertThrows(NoSuchElementException::class.java) { myServer.usersInviteToChat(user.userId + "_qwerty鷗", chatId.chatId, user.userId, token) }
        }

        @Test
        fun testNonExistingChat() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            val invUser = newDummyUser()
            getUserInfo(invUser)
            // Invite non-existing
            assertThrows(UserNotMemberException::class.java) { myServer.usersInviteToChat(invUser.userId, chatId.chatId - 30239566, user.userId, token) }
        }

        @Test
        fun testWrongToken() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)
            val invUser = newDummyUser()
            getUserInfo(invUser)

            // Invite non-existing
            assertThrows(UserNotAuthorizedException::class.java) { myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token + "_qwerty鷗") }
        }
    }

    @Nested
    inner class ChatsJoinTest {
        @Test
        fun testNormal() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)
            val listOfChats = myServer.usersListChats(user.userId, token)
            assertEquals(chatId, listOfChats.find { it == chatId })

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)

            myServer.chatsJoin(chatId.chatId, secret, invUser.userId, getToken(invUser))
            assert(myServer.usersListChats(invUser.userId, invToken).contains(chatId))
        }

        @Test
        fun testJoinJoined() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)

            myServer.chatsJoin(chatId.chatId, secret, invUser.userId, invToken)
            assert(myServer.usersListChats(invUser.userId, getToken(invUser)).contains(chatId))
            assertThrows(UserAlreadyMemberException::class.java) { myServer.chatsJoin(chatId.chatId, secret, invUser.userId, invToken) }
        }

        @Test
        fun testInviteToNonExisting() {
            val user = newDummyUser()
            val token = getToken(user)
            assertFails { myServer.chatsJoin(-123, "abcdef12", user.userId, token) }
        }

        @Test
        fun testWrongSecret() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, getToken(invUser), chatId)

            assertThrows(WrongChatSecretException::class.java) { myServer.chatsJoin(chatId.chatId, "$secret--qwerty", invUser.userId, invToken) }
        }

        @Test
        fun testWrongToken() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)

            assertThrows(UserNotAuthorizedException::class.java) { myServer.chatsJoin(chatId.chatId, secret, invUser.userId, invToken + "abc") }
        }
    }

    @Nested
    inner class ChatsLeaveTest {
        @Test
        fun testNormal() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)
            val listOfChats = myServer.usersListChats(user.userId, token)
            assertEquals(chatId, listOfChats.find { it == chatId })

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)

            myServer.chatsJoin(chatId.chatId, secret, invUser.userId, getToken(invUser))
            assert(myServer.usersListChats(invUser.userId, invToken).contains(chatId))

            myServer.chatsLeave(chatId.chatId, invUser.userId, getToken(invUser))
            assertFalse(myServer.usersListChats(invUser.userId, invToken).contains(chatId))
        }

        @Test
        fun testLeaveNotJoined() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)
            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            assertThrows(UserNotMemberException::class.java) { myServer.chatsLeave(chatId.chatId, invUser.userId, invToken) }
        }

        @Test
        fun testLeaveNonExisting() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            assertFails { myServer.chatsLeave(chatId.chatId, user.userId + "---qwerty", token) }
        }

        @Test
        fun testLeaveWrongToken() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)

            myServer.chatsJoin(chatId.chatId, secret, invUser.userId, invToken)
            assert(myServer.usersListChats(invUser.userId, invToken).contains(chatId))

            assertThrows(UserNotAuthorizedException::class.java) { myServer.chatsLeave(chatId.chatId, invUser.userId, invToken + "abc") }
        }

        @Test
        fun testLeaveNonExistingChat() {
            // Create init user
            val user = newDummyUser()
            val token = getToken(user)
            assertFails { myServer.chatsLeave(-123, user.userId, token) }
        }
    }

    @Nested
    inner class UsersListChatsTest {
        @Test
        fun testNormal() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)
            val listOfChats = myServer.usersListChats(user.userId, getToken(user))
            assertEquals(chatId, listOfChats.find { it == chatId })

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)
            myServer.chatsJoin(chatId.chatId, secret, invUser.userId, getToken(invUser))
            assert(myServer.usersListChats(invUser.userId, getToken(invUser)).contains(chatId))
            assert(myServer.usersListChats(invUser.userId, invToken).contains(chatId))

            assertEquals(2, myServer.usersListChats(invUser.userId, invToken).size)
        }

        @Test
        fun testWrongToken() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            assertThrows(UserNotAuthorizedException::class.java) { myServer.usersListChats(user.userId, token + "abc") }
        }

        @Test
        fun testNonExisting() {
            // Create init user and chat
            val user = newDummyUser()
            assertFails { myServer.usersListChats(user.userId, "qwerty") }
        }
    }

    @Nested
    inner class ChatsMembersListTest {
        @Test
        fun testNormal() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)
            val listOfChats = myServer.usersListChats(user.userId, token)
            assertEquals(chatId, listOfChats.find { it == chatId })

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)
            myServer.chatsJoin(chatId.chatId, secret, invUser.userId, invToken)

            val members0 = myServer.chatsMembersList(chatId.chatId, user.userId, getToken(user))
            assertEquals(2, members0.size)
            assertTrue(members0.any { it.userId == user.userId })
            assertTrue(members0.any { it.userId == invUser.userId })

            val members = myServer.chatsMembersList(chatId.chatId, user.userId, token)
            assertEquals(2, members.size)
            assertTrue(members.any { it.userId == user.userId })
            assertTrue(members.any { it.userId == invUser.userId })
        }

        @Test
        fun testNotJoined() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            assertThrows(UserNotMemberException::class.java) { myServer.chatsMembersList(chatId.chatId, invUser.userId, invToken) }
        }

        @Test
        fun testNonExistingPerson() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)
            assertFails { myServer.chatsMembersList(chatId.chatId, user.userId + "---qwerty", "abcdef12") }
        }

        @Test
        fun testWrongToken() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)
            myServer.chatsJoin(chatId.chatId, secret, invUser.userId, invToken)
            assertThrows(UserNotAuthorizedException::class.java) { myServer.chatsMembersList(chatId.chatId, user.userId, token + "abc") }
        }

        @Test
        fun testNonExistingChat() {
            val user = newDummyUser()
            val token = getToken(user)
            assertFails { myServer.chatsMembersList(-123, user.userId, token) }
        }
    }

    @Nested
    inner class ChatMessagesCreateAndListTest {
        @TestFactory
        fun testNormal(): Collection<DynamicTest> {
            val tests = listOf(
                    Triple("Standard", "Hello World!", 1),
                    Triple("Empty", "", 1),
                    Triple("Big and scary", "Ṱ̺̺̕o͞ ̷i̲̬͇̪͙n̝̗͕v̟̜̘̦͟o̶̙̰̠kè͚̮̺̪̹̱̤ ̖t̝͕̳̣̻̪͞h̼͓̲̦̳̘̲e͇̣̰̦̬͎ ̢̼̻̱̘h͚͎͙̜̣̲ͅi̦̲̣̰̤v̻͍e̺̭̳̪̰-m̢iͅn̖̺̞̲̯̰d̵̼̟͙̩̼̘̳ ̞̥̱̳̭r̛̗̘e͙p͠r̼̞̻̭̗e̺̠̣͟s̘͇̳͍̝͉e͉̥̯̞̲͚̬͜ǹ̬͎͎̟̖͇̤t͍̬̤͓̼̭͘ͅi̪̱n͠g̴͉ ͏͉ͅc̬̟h͡a̫̻̯͘o̫̟̖͍̙̝͉s̗̦̲.̨̹͈̣\n" +
                            "̡͓̞ͅI̗̘̦͝n͇͇͙v̮̫ok̲̫̙͈i̖͙̭̹̠̞n̡̻̮̣̺g̲͈͙̭͙̬͎ ̰t͔̦h̞̲e̢̤ ͍̬̲͖f̴̘͕̣è͖ẹ̥̩l͖͔͚i͓͚̦͠n͖͍̗͓̳̮g͍ ̨o͚̪͡f̘̣̬ ̖̘͖̟͙̮c҉͔̫͖͓͇͖ͅh̵̤̣͚͔á̗̼͕ͅo̼̣̥s̱͈̺̖̦̻͢.̛̖̞̠̫̰\n" +
                            "̗̺͖̹̯͓Ṯ̤͍̥͇͈h̲́e͏͓̼̗̙̼̣͔ ͇̜̱̠͓͍ͅN͕͠e̗̱z̘̝̜̺͙p̤̺̹͍̯͚e̠̻̠͜r̨̤͍̺̖͔̖̖d̠̟̭̬̝͟i̦͖̩͓͔̤a̠̗̬͉̙n͚͜ ̻̞̰͚ͅh̵͉i̳̞v̢͇ḙ͎͟-҉̭̩̼͔m̤̭̫i͕͇̝̦n̗͙ḍ̟ ̯̲͕͞ǫ̟̯̰̲͙̻̝f ̪̰̰̗̖̭̘͘c̦͍̲̞͍̩̙ḥ͚a̮͎̟̙͜ơ̩̹͎s̤.̝̝ ҉Z̡̖̜͖̰̣͉̜a͖̰͙̬͡l̲̫̳͍̩g̡̟̼̱͚̞̬ͅo̗͜.̟\n" +
                            "̦H̬̤̗̤͝e͜ ̜̥̝̻͍̟́w̕h̖̯͓o̝͙̖͎̱̮ ҉̺̙̞̟͈W̷̼̭a̺̪͍į͈͕̭͙̯̜t̶̼̮s̘͙͖̕ ̠̫̠B̻͍͙͉̳ͅe̵h̵̬͇̫͙i̹͓̳̳̮͎̫̕n͟d̴̪̜̖ ̰͉̩͇͙̲͞ͅT͖̼͓̪͢h͏͓̮̻e̬̝̟ͅ ̤̹̝W͙̞̝͔͇͝ͅa͏͓͔̹̼̣l̴͔̰̤̟͔ḽ̫.͕\n" +
                            "Z̮̞̠͙͔ͅḀ̗̞͈̻̗Ḷ͙͎̯̹̞͓G̻O̭̗̮", 1000)
            )

            return tests.map {
                dynamicTest("Message is ${it.first}.") {
                    // Create init user and chat
                    val user = newDummyUser()
                    val token = getToken(user)
                    val chatId = createChat(user, token)
                    val listOfChats = myServer.usersListChats(user.userId, token)
                    assertEquals(chatId, listOfChats.find { it == chatId })
                    // TODO add messages sending loop

                    // Create user, who will be invited
                    val invUser = newDummyUser()
                    val invToken = getToken(invUser)

                    // Invite
                    myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
                    val secret = getSecret(invUser, invToken, chatId)
                    myServer.chatsJoin(chatId.chatId, secret, invUser.userId, invToken)

                    myServer.chatMessagesCreate(chatId.chatId, it.second, user.userId, token)

                    val messages0 = myServer.chatMessagesList(chatId.chatId, user.userId, getToken(user))
                    assertEquals(1, messages0.size)
                    assertEquals(it.second, messages0[0].text)

                    val messages1 = myServer.chatMessagesList(chatId.chatId, user.userId, token)
                    assertEquals(1, messages1.size)
                    assertEquals(it.second, messages1[0].text)

                    val messages2 = myServer.chatMessagesList(chatId.chatId, invUser.userId, invToken)
                    assertEquals(1, messages2.size)
                    assertEquals(it.second, messages2[0].text)

                    //delay(1000) // чтобы сообщения не создались в одну секунду

                    val newMessage = it.second + " is worse than tea."
                    myServer.chatMessagesCreate(chatId.chatId, newMessage, invUser.userId, getToken(invUser))

                    val messages3 = myServer.chatMessagesList(chatId.chatId, user.userId, token)
                    assertEquals(2, messages3.size)
                    assertEquals(it.second, messages3[0].text)
                    assertEquals(newMessage, messages3[1].text)

                    val messages4 = myServer.chatMessagesList(chatId.chatId, invUser.userId,
                            getToken(invUser), messages3[0].messageId + 1)
                    val res = MessengerServer.storage.showMessages()
                    assertEquals(1, messages4.size)
                    assertEquals(newMessage, messages4[0].text)
                }
            }.toList()
        }

        @Test
        fun notJoined() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            assertThrows(UserNotMemberException::class.java) {
                myServer.chatMessagesCreate(chatId.chatId, "Want some tea?",
                        invUser.userId, invToken)
            }
        }

        @Test
        fun testNonExistingChat() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            assertFails { myServer.chatMessagesCreate(-123, "Want some tea?", user.userId, token) }
        }

        @Test
        fun testNonExisting() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            assertFails {
                myServer.chatMessagesCreate(chatId.chatId, "Want some tea?",
                        user.userId + "---qwerty", token)
            }
        }

        @Test
        fun testWrongToken() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)
            myServer.chatsJoin(chatId.chatId, secret, invUser.userId, invToken)

            assertThrows(UserNotAuthorizedException::class.java) {
                myServer.chatMessagesCreate(chatId.chatId,
                        "Want some tea?", user.userId, token + "abc")
            }
        }

        @Test
        fun testWrongAfterId() {
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            assertThrows(IllegalArgumentException::class.java) {
                myServer.chatMessagesList(chatId.chatId, user.userId,
                        token, -1)
            }
        }
    }

    @Nested
    inner class ChatMessagesDeleteByIdTest {
        @Test
        fun testNormal() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)
            val listOfChats = myServer.usersListChats(user.userId, token)
            assertEquals(chatId, listOfChats.find { it == chatId })

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            // Invite
            myServer.usersInviteToChat(invUser.userId, chatId.chatId, user.userId, token)
            val secret = getSecret(invUser, invToken, chatId)
            myServer.chatsJoin(chatId.chatId, secret, invUser.userId, invToken)

            val messageText = "Want some tea?"
            myServer.chatMessagesCreate(chatId.chatId, messageText, user.userId, token)
            var message = myServer.chatMessagesList(chatId.chatId, user.userId, token).find {
                it.text == messageText } ?: fail("Message should not be null!")

            myServer.chatMessagesDeleteById(message.messageId, user.userId, getToken(user))
            assertFalse(myServer.chatMessagesList(chatId.chatId, user.userId, getToken(user)).contains(message))
            assertFalse(myServer.chatMessagesList(chatId.chatId, invUser.userId, getToken(invUser)).contains(message))

            myServer.chatMessagesCreate(chatId.chatId, messageText, user.userId, token)
            message = myServer.chatMessagesList(chatId.chatId, user.userId, token).find {
                it.text == messageText } ?: fail("Message should not be null!")

            myServer.chatMessagesDeleteById(message.messageId, invUser.userId, getToken(invUser))
            assertFalse(myServer.chatMessagesList(chatId.chatId, user.userId, getToken(user)).contains(message))
            assertFalse(myServer.chatMessagesList(chatId.chatId, invUser.userId, getToken(invUser)).contains(message))
        }

        @Test
        fun testNonJoined() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            // Create user, who will be invited
            val invUser = newDummyUser()
            val invToken = getToken(invUser)

            val message = myServer.chatMessagesCreate(chatId.chatId, "Want some tea?", user.userId, token)

            assertFails { myServer.chatMessagesDeleteById(message.messageId, invUser.userId, invToken) }
        }

        @Test
        fun testNonExistingPerson() {
            // Create init user and chat
            val user = newDummyUser()
            val token = getToken(user)
            val chatId = createChat(user, token)

            val message = myServer.chatMessagesCreate(chatId.chatId, "Want some tea?", user.userId, token)

            assertFails { myServer.chatMessagesDeleteById(message.messageId, user.userId + "_abc", token) }
        }
    }

    @Nested
    inner class CheckUserAuthorizationTest {
        @Test
        fun testNormal() {
            val user = newDummyUser()
            val userInfo = getUserInfo(user)
            val token = getToken(user)
            assertEquals(userInfo, myServer.checkUserAuthorization(user.userId, token))
        }

        @Test
        fun testWrongToken() {
            val user = newDummyUser()
            val token = getToken(user)
            assertThrows(UserNotAuthorizedException::class.java) { myServer.checkUserAuthorization(user.userId, token + "abc") }
        }

        @Test
        fun testNonExisting() {
            val user = newDummyUser()
            assertFails { myServer.checkUserAuthorization(user.userId, "abcdef12") }
        }
    }

    @Test
    fun testGetSystemUser() {
        // Create init user and chat
        val user = newDummyUser()
        val token = getToken(user)

        val systemId = myServer.getSystemUserId()
        val systemUser = myServer.getSystemUser()
        assertEquals(systemUser.userId, systemId)

        val chats = myServer.usersListChats(user.userId, token)
        assertEquals(1, chats.size)
        val chat = chats[0].chatId
        assertNotNull(myServer.chatsMembersList(chat, user.userId, token).find { it.userId == systemId })
    }
}