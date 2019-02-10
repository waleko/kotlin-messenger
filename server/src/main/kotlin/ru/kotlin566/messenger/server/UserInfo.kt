package ru.kotlin566.messenger.server

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


/**
 * Пользователь
 */
@JsonIgnoreProperties("passwordHash")
data class UserInfo(val userId: String,
                    val displayName: String,
                    @field:JsonProperty("passwordHash")
                    val passwordHash: String)

data class NewUserInfo(val userId: String,
                       val displayName: String,
                       val password: String)

data class PasswordInfo(val password: String)