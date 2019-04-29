package ru.kotlin566.messenger.android_client.ui.login

/**
 * Authentication result : success (user details) or error message_view_item.
 */
data class LoginResult(
    val success: LoggedInUserView? = null,
    val error: Int? = null
)
