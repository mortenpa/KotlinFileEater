package com.hrblizz.fileapi.data.entities

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document(collection = "files")
class FileEntity {
    @Id
    lateinit var uuid: UUID
    lateinit var filename: String
    lateinit var originalFilename: String
    var upload_date: Date = Date()
    var fileExtension: String? = null
    var fileSize: Long = 0

    override fun toString(): String {
        return "FileEntity(uuid='$uuid', filename='$originalFilename', upload_date='${upload_date}')"
    }
}