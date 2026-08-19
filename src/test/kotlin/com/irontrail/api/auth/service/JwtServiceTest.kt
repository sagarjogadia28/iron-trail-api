package com.irontrail.api.auth.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Date

class JwtServiceTest {

    private val secret = "a-test-secret-that-is-long-enough-for-hs384-signing-1234567890"

    private fun service(expiration: Duration = Duration.ofDays(1)) =
        JwtService(secret, expiration)

    @Test
    fun `generateToken then extractUserId round-trips the same id`() {
        val jwtService = service()

        val token = jwtService.generateToken(42L)
        val extracted = jwtService.extractUserId(token)

        assertEquals(42L, extracted)
    }

    @Test
    fun `expiresInSeconds reflects the configured expiration`() {
        val jwtService = service(Duration.ofDays(30))

        assertEquals(Duration.ofDays(30).toSeconds(), jwtService.expiresInSeconds)
    }

    @Test
    fun `extractUserId returns null for an expired token`() {
        val jwtService = service(Duration.ofMillis(1))
        val token = jwtService.generateToken(1L)

        Thread.sleep(25)

        assertNull(jwtService.extractUserId(token))
    }

    @Test
    fun `extractUserId returns null when signature does not match`() {
        val issuer = service()
        val verifier = JwtService("a-completely-different-secret-value-1234567890-xyz", Duration.ofDays(1))

        val token = issuer.generateToken(7L)

        assertNull(verifier.extractUserId(token))
    }

    @Test
    fun `extractUserId returns null for a malformed token string`() {
        val jwtService = service()

        assertNull(jwtService.extractUserId("not-a-real-jwt"))
    }

    @Test
    fun `extractUserId returns null for an empty string`() {
        val jwtService = service()

        assertNull(jwtService.extractUserId(""))
    }

    @Test
    fun `extractUserId returns null when a real token is tampered with`() {
        val jwtService = service()
        val token = jwtService.generateToken(9L)

        val parts = token.split(".")
        val tamperedPayload = parts[1].dropLast(1) + if (parts[1].last() == 'A') 'B' else 'A'
        val tampered = "${parts[0]}.$tamperedPayload.${parts[2]}"

        assertNull(jwtService.extractUserId(tampered))
    }

    @Test
    fun `extractUserId returns null when subject claim is not numeric`() {
        val jwtService = service()
        val signingKey = Keys.hmacShaKeyFor(secret.toByteArray())
        val now = Date()
        val forged = Jwts.builder()
            .subject("not-a-number")
            .issuedAt(now)
            .expiration(Date(now.time + 60_000))
            .signWith(signingKey)
            .compact()

        assertNull(jwtService.extractUserId(forged))
    }

    @Test
    fun `construction fails fast with a secret too short for HMAC signing`() {
        assertThrows(Exception::class.java) {
            JwtService("short", Duration.ofDays(1))
        }
    }
}
