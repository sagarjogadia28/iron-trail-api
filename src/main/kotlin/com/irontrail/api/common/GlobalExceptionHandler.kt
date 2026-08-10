package com.irontrail.api.common

import com.irontrail.api.auth.exception.EmailAlreadyInUseException
import com.irontrail.api.exercise.exception.ExerciseNotFoundException
import com.irontrail.api.split.exception.SplitNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: Instant = Instant.now()
)

data class ValidationErrorResponse(
    val status: Int,
    val errors: Map<String, String>,
    val timestamp: Instant = Instant.now()
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ExerciseNotFoundException::class)
    fun handleNotFound(ex: ExerciseNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(status = HttpStatus.NOT_FOUND.value(), message = ex.message ?: "Not found"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ValidationErrorResponse(status = HttpStatus.BAD_REQUEST.value(), errors = errors))
    }

    @ExceptionHandler(EmailAlreadyInUseException::class)
    fun handleEmailInUse(ex: EmailAlreadyInUseException) : ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(status = HttpStatus.CONFLICT.value(), message = ex.message ?: "Email already in use"))

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException) : ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(status = HttpStatus.UNAUTHORIZED.value(), message = "Invalid email or password"))

    @ExceptionHandler(SplitNotFoundException::class)
    fun handleSplitNotFound(ex: SplitNotFoundException) : ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(status = HttpStatus.NOT_FOUND.value(), message = ex.message ?: "Not found"))
}