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
        val email = request.email.trim().lowercase()
        if (userRepository.findByEmail(email) != null) {
            throw EmailAlreadyInUseException(email)
        }

        val user = User(
            email = email,
            passwordHash = passwordEncoder.encode(request.password)!!
        )
        userRepository.save(user)
        return AuthResponse(jwtService.generateToken(user.userId))
    }

    fun login(request: LoginRequest) : AuthResponse {
        val email = request.email.trim().lowercase()
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(email, request.password)
        )

        val user = userRepository.findByEmail(email)
            ?: throw IllegalStateException("Authenticated user not found: $email")

        return AuthResponse(jwtService.generateToken(user.userId))
    }

}