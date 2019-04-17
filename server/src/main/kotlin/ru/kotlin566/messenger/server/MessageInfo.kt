package ru.kotlin566.messenger.server

import java.time.Instant

/**
 * Сообщение
 */
data class MessageInfo (val messageId: Int, val memberId: Int, var text: String,  val createdOn: Long = Instant.now().epochSecond) {
    // TODO: createdOn must be internal!


}

data class NewMessageInfo (var text: String)
