package com.hrblizz.fileapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class StatusController(
) {
    @RequestMapping("/api/status", method = [RequestMethod.GET])
    fun getStatus(): ResponseEntity<Any> {

        return ResponseEntity(
            mapOf(
                "message" to "API is operational!"
            ),
            HttpStatus.OK
        )
    }
}
