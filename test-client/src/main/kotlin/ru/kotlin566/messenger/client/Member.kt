package ru.kotlin566.messenger.client

/**
 * Участник чата
 */
class Member(val memberId: Int, val displayName: String, val memberUserId: String, chat: Chat) : ChatAware(chat)