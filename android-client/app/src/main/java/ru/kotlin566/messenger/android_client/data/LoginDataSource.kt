package ru.kotlin566.messenger.android_client.data

import ru.kotlin566.messenger.android_client.RequestsHandler
import ru.kotlin566.messenger.android_client.data.model.LoggedInUser
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(username: String, password: String): Result<LoggedInUser> {
        try {
            // TODO: handle loggedInUser authentication
            val token = RequestsHandler.login(username, password)
//            val user = RequestsHandler.usersListById(username, username, token).first()
//
//            val fakeUser = LoggedInUser(username, user.)
            val fakeUser = LoggedInUser(username, username, token)
            return Result.Success(fakeUser)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}

