package com.irontrail.api.auth.exception

class EmailAlreadyInUseException(email: String) : RuntimeException("Email already in use: $email")