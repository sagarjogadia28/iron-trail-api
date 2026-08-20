package com.irontrail.api.auth.service

import com.irontrail.api.user.model.User
import com.irontrail.api.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException

class CustomUserDetailsServiceTest {
    private val userRepository: UserRepository = mock()
    private val service = CustomUserDetailsService(userRepository)

    @Test
    fun `loadUserByUsername returns UserDetails with the stored password hash and ROLE_USER`() {
        whenever(userRepository.findByEmail("john@gmail.com"))
            .thenReturn(User(email = "john@gmail.com", passwordHash = "bcrypt-hash").apply { userId = 1L })

        val details = service.loadUserByUsername("john@gmail.com")

        assertEquals("john@gmail.com", details.username)
        assertEquals("bcrypt-hash", details.password)
        assertTrue(details.authorities.any { it.authority == "ROLE_USER" })
    }

    @Test
    fun `loadUserByUsername throws UsernameNotFoundException when no user matches`() {
        whenever(userRepository.findByEmail("ghost@gmail.com")).thenReturn(null)

        assertThrows(UsernameNotFoundException::class.java) {
            service.loadUserByUsername("ghost@gmail.com")
        }
    }

    @Test
    fun `loadUserByUsername does not itself normalize case - it looks up exactly what it was given`() {
        whenever(userRepository.findByEmail("John@Gmail.com")).thenReturn(null)

        assertThrows(UsernameNotFoundException::class.java) {
            service.loadUserByUsername("John@Gmail.com")
        }
        org.mockito.kotlin
            .verify(userRepository)
            .findByEmail("John@Gmail.com")
    }
}
