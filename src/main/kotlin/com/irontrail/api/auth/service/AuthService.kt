package com.irontrail.api.auth.service

import com.irontrail.api.auth.dto.AuthResponse
import com.irontrail.api.auth.dto.LoginRequest
import com.irontrail.api.auth.dto.RegisterRequest
import com.irontrail.api.auth.exception.EmailAlreadyInUseException
import com.irontrail.api.user.model.User
import com.irontrail.api.user.repository.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService
) {

    fun register(request: RegisterRequest) : AuthResponse {
        if (userRepository.findByEmail(request.email) != null) {
            throw EmailAlreadyInUseException(request.email)
        }

        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)!!
        )
        userRepository.save(user)
        return AuthResponse(jwtService.generateToken(user.userId))
    }

    fun login(request: LoginRequest) : AuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalStateException("Authenticated user not found: ${request.email}")

        return AuthResponse(jwtService.generateToken(user.userId))
    }

}