package com.irontrail.api.auth.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value($$"${jwt.secret}") secret: String,
    @Value($$"${jwt.expiration}") private val expiration: Duration
) {

    private val signingKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(userId: Long): String {
        val now = Date()
        val expiry = Date(now.time + expiration.toMillis())

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }

    fun extractUserId(token: String): Long? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
            claims.subject.toLong()
        } catch (e: Exception) {
            null
        }
    }
}