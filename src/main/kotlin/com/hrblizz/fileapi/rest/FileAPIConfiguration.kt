package com.hrblizz.fileapi.rest

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "files-api")
class FileAPIConfiguration {
    val DEFAULT_MIME_TYPE = "application/octet-stream"
    // upload limit per file, defaulting to 10MB
    var maxFileSize: Long = 1048576
    var fileDirectory: String = "uploads"
    var limitedFiletypes: List<String> = mutableListOf()
    var fileTypeLimitingIncludes: Boolean = true
}