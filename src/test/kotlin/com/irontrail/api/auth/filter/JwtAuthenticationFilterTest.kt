package com.irontrail.api.auth.filter

import com.irontrail.api.auth.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest {

    private val jwtService: JwtService = mock()
    private val filter = JwtAuthenticationFilter(jwtService)
    private val request: HttpServletRequest = mock()
    private val response: HttpServletResponse = mock()
    private val filterChain: FilterChain = mock()

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `sets an authenticated principal from a valid Bearer token`() {
        whenever(request.getHeader("Authorization")).thenReturn("Bearer valid-token")
        whenever(jwtService.extractUserId("valid-token")).thenReturn(42L)

        filter.doFilter(request, response, filterChain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals(42L, auth?.principal)
        assertEquals(true, auth?.isAuthenticated)
        assertEquals(listOf(SimpleGrantedAuthority("ROLE_USER")), auth?.authorities?.toList())
    }

    @Test
    fun `always continues the filter chain, token present or not`() {
        whenever(request.getHeader("Authorization")).thenReturn("Bearer valid-token")
        whenever(jwtService.extractUserId("valid-token")).thenReturn(42L)

        filter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `leaves the context unauthenticated when there's no Authorization header`() {
        whenever(request.getHeader("Authorization")).thenReturn(null)

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `leaves the context unauthenticated when the Authorization header isn't a Bearer token`() {
        whenever(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz")

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `leaves the context unauthenticated when the token is invalid or expired`() {
        whenever(request.getHeader("Authorization")).thenReturn("Bearer garbage-token")
        whenever(jwtService.extractUserId("garbage-token")).thenReturn(null)

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `does not overwrite an authentication already present in the context`() {
        // Defends the "if authentication == null" guard - a real request only ever reaches this
        // filter once, but this proves the filter itself won't clobber pre-existing auth if it did.
        val existing = UsernamePasswordAuthenticationToken(999L, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
        SecurityContextHolder.getContext().authentication = existing
        whenever(request.getHeader("Authorization")).thenReturn("Bearer valid-token")
        whenever(jwtService.extractUserId("valid-token")).thenReturn(42L)

        filter.doFilter(request, response, filterChain)

        assertEquals(999L, SecurityContextHolder.getContext().authentication?.principal)
    }

    @Test
    fun `strips exactly the 'Bearer ' prefix before extracting the token`() {
        whenever(request.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi")
        whenever(jwtService.extractUserId("abc.def.ghi")).thenReturn(1L)

        filter.doFilter(request, response, filterChain)

        verify(jwtService).extractUserId("abc.def.ghi")
    }
}
