package com.irontrail.api.auth.exception

import com.irontrail.api.common.ConflictException

class EmailAlreadyInUseException(
    email: String,
) : ConflictException("Email already in use: $email")
