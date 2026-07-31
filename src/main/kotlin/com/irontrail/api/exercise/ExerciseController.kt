package com.irontrail.api.exercise

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/exercises")
class ExerciseController(
    private val exerciseService: ExerciseService
) {

    @GetMapping
    fun findAll(@RequestParam(required = false) muscleGroup: MuscleGroup?): List<ExerciseResponse> =
        exerciseService.findAll(muscleGroup)

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ExerciseResponse = exerciseService.findById(id)

    @PostMapping
    fun create(@Valid @RequestBody request: ExerciseRequest): ResponseEntity<ExerciseResponse> {
        val created = exerciseService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: ExerciseRequest): ExerciseResponse =
        exerciseService.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        exerciseService.delete(id)
        return ResponseEntity.noContent().build()
    }
}