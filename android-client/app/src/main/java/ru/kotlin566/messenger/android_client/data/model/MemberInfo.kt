package ru.kotlin566.messenger.android_client.data.model

/**
 * Участник чата
 */
data class MemberInfo(val memberId: Int, val chatId: Int, val chatDisplayName: String, val memberDisplayName: String, val userId: String)