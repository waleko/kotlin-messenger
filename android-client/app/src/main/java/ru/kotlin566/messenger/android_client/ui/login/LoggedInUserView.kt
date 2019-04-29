package ru.kotlin566.messenger.android_client.ui.login

/**
 * User details post authentication that is exposed to the UI
 */
data class LoggedInUserView(
    val username: String,
    val displayName: String
    //... other data fields that may be accessible to the UI
)
