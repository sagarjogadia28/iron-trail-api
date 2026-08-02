package com.irontrail.api.exercise.exception

class ExerciseNotFoundException(id: Long) : RuntimeException("Exercise not found: $id")