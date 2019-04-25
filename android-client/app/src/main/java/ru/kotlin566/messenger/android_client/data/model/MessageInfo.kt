package ru.kotlin566.messenger.android_client.data.model

import java.time.Instant

/**
 * Сообщение
 */
data class MessageInfo (val messageId: Int, val memberId: Int, var text: String) {
    // TODO: createdOn must be internal!
    val createdOn = Instant.now()
}

data class NewMessageInfo (var text: String)
