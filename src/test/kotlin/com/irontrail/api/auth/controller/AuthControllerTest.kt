package com.irontrail.api.auth.controller

import com.irontrail.api.auth.dto.AuthResponse
import com.irontrail.api.auth.dto.LoginRequest
import com.irontrail.api.auth.dto.RegisterRequest
import com.irontrail.api.auth.exception.EmailAlreadyInUseException
import com.irontrail.api.auth.service.AuthService
import com.irontrail.api.testsupport.standaloneMvcBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

// No authenticateAs() anywhere here - /v1/auth/** is deliberately the one publicly-reachable
// route group (SecurityConfig permits it), and register/login are how a caller gets a token in
// the first place.
class AuthControllerTest {

    private val authService: AuthService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(AuthController(authService)).build()

    // ---- register ----

    @Test
    fun `POST register with a valid body returns 201 with the token`() {
        val request = RegisterRequest(email = "sagar@test.com", password = "password123")
        whenever(authService.register(request)).thenReturn(AuthResponse(accessToken = "jwt-token", expiresIn = 2592000))

        mockMvc.perform(
            post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"sagar@test.com","password":"password123"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(2592000))
    }

    @Test
    fun `POST register with a duplicate email returns 409`() {
        whenever(authService.register(any())).thenThrow(EmailAlreadyInUseException("sagar@test.com"))

        mockMvc.perform(
            post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"sagar@test.com","password":"password123"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Email already in use: sagar@test.com"))
    }

    @Test
    fun `POST register with a malformed email returns 400 and never calls the service`() {
        mockMvc.perform(
            post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"not-an-email","password":"password123"}""")
        ).andExpect(status().isBadRequest)

        verify(authService, never()).register(any())
    }

    @Test
    fun `POST register with a password under 8 characters returns 400`() {
        mockMvc.perform(
            post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"sagar@test.com","password":"short"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST register with a password over 72 characters returns 400`() {
        val tooLong = "a".repeat(73)

        mockMvc.perform(
            post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"sagar@test.com","password":"$tooLong"}""")
        ).andExpect(status().isBadRequest)
    }

    // ---- login ----

    @Test
    fun `POST login with valid credentials returns 200 with the token`() {
        val request = LoginRequest(email = "sagar@test.com", password = "password123")
        whenever(authService.login(request)).thenReturn(AuthResponse(accessToken = "jwt-token", expiresIn = 2592000))

        mockMvc.perform(
            post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"sagar@test.com","password":"password123"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
    }

    @Test
    fun `POST login with wrong credentials returns 401 with a generic message`() {
        whenever(authService.login(any())).thenThrow(BadCredentialsException("irrelevant"))

        mockMvc.perform(
            post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"sagar@test.com","password":"wrong"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid email or password"))
    }

    @Test
    fun `POST login with a blank password returns 400 and never calls the service`() {
        mockMvc.perform(
            post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"sagar@test.com","password":""}""")
        ).andExpect(status().isBadRequest)

        verify(authService, never()).login(any())
    }
}
