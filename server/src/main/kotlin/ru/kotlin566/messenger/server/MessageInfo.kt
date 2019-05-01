package ru.kotlin566.messenger.server

import java.time.Instant

/**
 * Сообщение
 */
data class MessageInfo (val messageId: Int, val memberId: Int, var text: String, val createdOn : Instant = Instant.now()) {
    // TODO: createdOn must be internal!
    // Sorry, I've just changed it for my purposes
}

data class NewMessageInfo (var text: String)
