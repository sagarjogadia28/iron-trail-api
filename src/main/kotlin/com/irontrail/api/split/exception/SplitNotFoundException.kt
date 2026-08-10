package com.irontrail.api.split.exception

class SplitNotFoundException(splitId: Long) : RuntimeException("Split not found: $splitId")