package com.irontrail.api.testsupport

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder

// @WebMvcTest could not be made to reliably enforce SecurityConfig's authorizeHttpRequests rule or
// resolve @AuthenticationPrincipal in this project's exact Spring Boot 4.1/Security 7.1 setup - the
// narrow slice's ApplicationContext never assembles a real "springSecurityFilterChain" bean, even
// though the same setup works correctly in the full app (curl-verified). standaloneSetup() sidesteps
// the whole filter-chain question: it wires the controller directly against a mocked service, with
// the real AuthenticationPrincipalArgumentResolver and GlobalExceptionHandler registered explicitly,
// and authentication driven straight through SecurityContextHolder rather than through a servlet
// filter chain that isn't there. It cannot exercise the "no token -> 403" cross-cutting rule itself -
// that's SecurityConfig's job, already covered end-to-end via curl, not this controller's.
fun standaloneMvcBuilder(controller: Any): StandaloneMockMvcBuilder =
    MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(
            com.irontrail.api.common
                .GlobalExceptionHandler(),
        ).setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())

fun authenticateAs(userId: Long) {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
}

fun clearAuthentication() {
    SecurityContextHolder.clearContext()
}
