package com.irontrail.api.auth.service

import com.irontrail.api.auth.dto.LoginRequest
import com.irontrail.api.auth.dto.RegisterRequest
import com.irontrail.api.auth.exception.EmailAlreadyInUseException
import com.irontrail.api.user.model.User
import com.irontrail.api.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {
    private val userRepository: UserRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val authenticationManager: AuthenticationManager = mock()
    private val jwtService: JwtService = mock()

    private val authService = AuthService(userRepository, passwordEncoder, authenticationManager, jwtService)

    private fun savedUser(
        id: Long = 1L,
        email: String = "john@gmail.com",
    ) = User(email = email, passwordHash = "hashed").apply { userId = id }

    @BeforeEach
    fun stubJwt() {
        whenever(jwtService.generateToken(any())).thenReturn("signed-token")
        whenever(jwtService.expiresInSeconds).thenReturn(2_592_000L)
    }

    // ---- register ----

    @Test
    fun `register normalizes email case and whitespace before checking uniqueness`() {
        whenever(userRepository.findByEmail("john@gmail.com")).thenReturn(null)
        whenever(passwordEncoder.encode("password123")).thenReturn("hashed")
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        authService.register(RegisterRequest("  John@Gmail.com  ", "password123"))

        verify(userRepository).findByEmail("john@gmail.com")
    }

    @Test
    fun `register rejects a duplicate email case-insensitively`() {
        whenever(userRepository.findByEmail("john@gmail.com")).thenReturn(savedUser())

        assertThrows(EmailAlreadyInUseException::class.java) {
            authService.register(RegisterRequest("JOHN@gmail.com", "password123"))
        }

        verify(userRepository, org.mockito.kotlin.never()).save(any())
        verifyNoInteractions(passwordEncoder)
        verifyNoInteractions(jwtService)
    }

    @Test
    fun `register stores the encoded password, never the raw password`() {
        whenever(userRepository.findByEmail(any())).thenReturn(null)
        whenever(passwordEncoder.encode("plaintext-pw")).thenReturn("bcrypt-hash")
        val captor = argumentCaptor<User>()
        whenever(userRepository.save(captor.capture())).thenAnswer { it.arguments[0] as User }

        authService.register(RegisterRequest("new@user.com", "plaintext-pw"))

        assertEquals("bcrypt-hash", captor.firstValue.passwordHash)
    }

    @Test
    fun `register returns a token and expiry sourced from JwtService for the saved user id`() {
        whenever(userRepository.findByEmail(any())).thenReturn(null)
        whenever(passwordEncoder.encode(any())).thenReturn("hashed")
        whenever(userRepository.save(any())).thenAnswer {
            (it.arguments[0] as User).apply { userId = 55L }
        }
        whenever(jwtService.generateToken(55L)).thenReturn("token-for-55")

        val response = authService.register(RegisterRequest("new@user.com", "password123"))

        assertEquals("token-for-55", response.accessToken)
        assertEquals(2_592_000L, response.expiresIn)
        assertEquals("Bearer", response.tokenType)
    }

    // ---- login ----

    @Test
    fun `login normalizes email before authenticating and looking up the user`() {
        whenever(userRepository.findByEmail("john@gmail.com")).thenReturn(savedUser())
        val captor = argumentCaptor<UsernamePasswordAuthenticationToken>()
        whenever(authenticationManager.authenticate(captor.capture())).thenReturn(null)

        authService.login(LoginRequest("  John@Gmail.com ", "password123"))

        assertEquals("john@gmail.com", captor.firstValue.principal)
    }

    @Test
    fun `login propagates authentication failure without touching the repository or jwt service afterward`() {
        whenever(authenticationManager.authenticate(any()))
            .thenThrow(BadCredentialsException("bad creds"))

        assertThrows(BadCredentialsException::class.java) {
            authService.login(LoginRequest("john@gmail.com", "wrong-password"))
        }

        verifyNoInteractions(jwtService)
    }

    @Test
    fun `login throws IllegalStateException if the user vanishes between authentication and lookup`() {
        whenever(authenticationManager.authenticate(any())).thenReturn(null)
        whenever(userRepository.findByEmail("john@gmail.com")).thenReturn(null)

        assertThrows(IllegalStateException::class.java) {
            authService.login(LoginRequest("john@gmail.com", "password123"))
        }

        verifyNoInteractions(jwtService)
    }

    @Test
    fun `login returns a token for the authenticated user's id`() {
        whenever(authenticationManager.authenticate(any())).thenReturn(null)
        whenever(userRepository.findByEmail("john@gmail.com")).thenReturn(savedUser(id = 99L))
        whenever(jwtService.generateToken(99L)).thenReturn("token-for-99")

        val response = authService.login(LoginRequest("john@gmail.com", "password123"))

        assertEquals("token-for-99", response.accessToken)
    }
}
