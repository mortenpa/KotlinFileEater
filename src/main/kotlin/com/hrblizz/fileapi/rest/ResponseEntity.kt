package com.hrblizz.fileapi.rest

import org.springframework.http.HttpStatus

class ResponseEntity<T> (
    val status: HttpStatus,
    val data: T? = null,
    val errors: List<ErrorMessage>? = null
)