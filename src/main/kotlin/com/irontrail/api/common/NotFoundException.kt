package com.irontrail.api.common

class NotFoundException(resource: String, id: Long) : RuntimeException("$resource not found: $id")