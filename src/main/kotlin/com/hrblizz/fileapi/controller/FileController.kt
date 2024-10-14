package com.hrblizz.fileapi.controller

import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.models.FileMetaRequest
import com.hrblizz.fileapi.data.repository.FileRepository
import com.hrblizz.fileapi.library.log.LogItem
import com.hrblizz.fileapi.library.log.Logger
import com.hrblizz.fileapi.rest.ErrorMessage
import com.hrblizz.fileapi.rest.FileAPIConfiguration
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import org.springframework.http.ResponseEntity



@RestController
@RequestMapping("/api")
class FileController(
    private val fileRepository: FileRepository,
    private val apiConfiguration: FileAPIConfiguration,
    private val logger: Logger
) {

    // Fetch file metadata based on the list of tokens
    @PostMapping("/files/metas")
    fun fetchFileMetas(@RequestBody request: FileMetaRequest): ResponseEntity<List<FileEntity>> {
        val fileMetaData = fileRepository.findAllByUuidIn(request.tokens)

        return ResponseEntity(
            fileMetaData,
            HttpStatus.OK
        )
    }

    // fetch metadata for all the files
    @GetMapping("/files/metas")
    fun getFileMetas(): ResponseEntity<List<FileEntity>> {
        val fileMetaData = fileRepository.findAll()

        return ResponseEntity(
            fileMetaData,
            HttpStatus.OK
        )
    }

    // download a file
    @GetMapping("/files/{token}")
    fun getFile(@PathVariable("token") token: UUID): ResponseEntity<Any> {

        val fileEntityOptional = fileRepository.findById(token)
        if (!fileEntityOptional.isPresent) {
            return ResponseEntity(
                ErrorMessage("Missing token"),
                HttpStatus.BAD_REQUEST)
        }

        val fileEntity = fileEntityOptional.get()
        val filePath = Paths.get(apiConfiguration.fileDirectory).resolve(fileEntity.filename)

        val fileExists = Files.exists(filePath)
        if (!fileExists) {
            logger.crit(LogItem("Failed to save file (${fileEntity.filename}, ${fileEntity.mimeType}, " +
                    "${fileEntity.fileExtension}, ${fileEntity.fileSize})", token.toString()))

            return ResponseEntity(
                ErrorMessage("Something went wrong with fetching the file"),
                HttpStatus.SERVICE_UNAVAILABLE)
        }

        // Read the file as an InputStream
        val inputStream = Files.newInputStream(filePath)
        val resource = InputStreamResource(inputStream)

        val headers = HttpHeaders()
        val mimeType = fileEntity.mimeType ?: apiConfiguration.DEFAULT_MIME_TYPE
        headers.set(HttpHeaders.CONTENT_TYPE, mimeType)
        headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
        headers.set("Content-Disposition", "attachment; filename=${fileEntity.filename}")

        // Return the InputStreamResource
        return ResponseEntity(resource, headers, HttpStatus.OK)
    }

    fun isValidFileMimeType(mimeType: String): Boolean {
        val allowedTypesList = apiConfiguration.limitedFiletypes
        if (allowedTypesList.isNotEmpty()) {
            return if (apiConfiguration.fileTypeLimitingIncludes) {
                mimeType in apiConfiguration.limitedFiletypes
            } else {
                mimeType !in apiConfiguration.limitedFiletypes
            }
        }
        // default to true
        return true
    }

    // upload a file
    @PostMapping("/files")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<Any> {

        if (file.isEmpty) {
            return ResponseEntity(
                ErrorMessage("Uploaded file is empty"),
                HttpStatus.BAD_REQUEST
            )
        }

        // check and remember mimeType
        val mimeType = file.contentType ?: apiConfiguration.DEFAULT_MIME_TYPE
        if (!isValidFileMimeType(mimeType)) {
            return ResponseEntity(
                ErrorMessage("Invalid file type"),
                HttpStatus.BAD_REQUEST
            )
        }

        // check file size
        if (file.size > apiConfiguration.maxFileSize) {
            return ResponseEntity(
                ErrorMessage("File exceeds maximum file size of ${apiConfiguration.maxFileSize}"),
                HttpStatus.BAD_REQUEST
            )
        }


        val uuid = UUID.randomUUID();
        val fileName = uuid.toString() + "_" + file.originalFilename
        val filePath = Paths.get(apiConfiguration.fileDirectory).resolve(fileName)

        val fileEntity = FileEntity().also {
            it.uuid = uuid
            it.filename = fileName
            it.originalFilename = file.originalFilename
            it.mimeType = mimeType
            it.uploadDate = Date()
            it.fileExtension = file.originalFilename?.substringAfterLast('.', "")
            it.fileSize = file.size
        }

        return try {
            Files.write(filePath, file.bytes, StandardOpenOption.CREATE_NEW)

            fileRepository.save(fileEntity)
            ResponseEntity(
                mapOf(
                    "uuid" to uuid
                ),
                HttpStatus.OK
            )
        } catch (exception: IOException) {
            logger.crit(
                LogItem(
                    "Failed to save file (${fileEntity.filename}, ${fileEntity.mimeType}, " +
                            "${fileEntity.fileExtension}, ${fileEntity.fileSize}): ${exception.message}",
                    uuid.toString()
                )
            )
            ResponseEntity(
                listOf(ErrorMessage("Error on saving the file")),
                HttpStatus.SERVICE_UNAVAILABLE
            )
        }
    }


    // file deletion
    @DeleteMapping("/files/{token}")
    fun deleteFile(@PathVariable("token") token: UUID): ResponseEntity<Any> {

        val fileMeta = fileRepository.findById(token)

        if (!fileMeta.isPresent) {
            return ResponseEntity(
                ErrorMessage("File with the token not found"),
                HttpStatus.BAD_REQUEST
            )
        }


        val filePath = Paths.get("${apiConfiguration.fileDirectory}/${fileMeta.get().filename}")
        return try {
            // Attempt to delete the file
            Files.delete(filePath)
            fileRepository.deleteById(token)

            return ResponseEntity(
                mapOf(
                    "deleted" to true
                ),
                HttpStatus.OK
            )
        } catch (ex: NoSuchFileException) {
            // File does not exist, return not found response
            ResponseEntity(
                ErrorMessage("File not found on server"),
                HttpStatus.BAD_REQUEST
            )
        } catch (ex: IOException) {
            logger.crit(LogItem("Failed to delete file with token $token: ${ex.message}", token.toString()))
            // Handle other IO exceptions
            ResponseEntity(
                ErrorMessage("Error occurred while deleting the file: ${ex.message}"),
                HttpStatus.SERVICE_UNAVAILABLE
            )
        }
    }
}