package ru.kotlin566.messenger.server

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import java.util.*

object JwtConfig {

    private const val secret = "KhkTJVBMKTJTYkKYIHik" // FIXME: move to application.properties
    private const val issuer = "kotlin-messenger"
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
            .require(algorithm)
            .withIssuer(issuer)
            .build()

    fun makeToken(userId: String, sessionToken: String): String = JWT.create()
            .withSubject("Authentication")
            .withIssuer(issuer)
            .withClaim("id", userId)
            .withClaim("session", sessionToken)
            .sign(algorithm)
}