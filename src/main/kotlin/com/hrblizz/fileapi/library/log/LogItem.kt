package com.hrblizz.fileapi.library.log

import java.time.LocalDateTime

open class LogItem constructor(
    val message: String,
    val correlationId: String? = null
) {
    val dateTime: LocalDateTime = LocalDateTime.now()

    var type: String? = null

    override fun toString(): String {
        return "[$dateTime] [$correlationId] $message"
    }
}
