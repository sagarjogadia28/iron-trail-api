package com.irontrail.api.exercise

class ExerciseNotFoundException(id: Long) : RuntimeException("Exercise not found: $id")