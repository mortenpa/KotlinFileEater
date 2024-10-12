package com.hrblizz.fileapi.controller

import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileRepository
import com.hrblizz.fileapi.rest.ErrorMessage
import com.hrblizz.fileapi.rest.ResponseEntity
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import org.springframework.http.ResponseEntity as SpringResponseEntity

@RestController
@RequestMapping("/api")
class FileController(
    private val fileRepository: FileRepository
) {

    private val fileDirectory = Paths.get("uploads")

    @GetMapping("/files/metas")
    fun getFileMetas(): ResponseEntity<List<FileEntity>> {
        val fileMetaData = fileRepository.findAll()

        return ResponseEntity(
            fileMetaData, HttpStatus.OK.value()
        )
    }

    @GetMapping("/files/{token}")
    fun getFile(@PathVariable("token") token: UUID): SpringResponseEntity<InputStreamResource> {

        val fileEntityOptional = fileRepository.findById(token)
        if (!fileEntityOptional.isPresent) {
            return SpringResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
        }

        val fileEntity = fileEntityOptional.get()
        val filePath = fileDirectory.resolve(fileEntity.filename)

        val fileExists = Files.exists(filePath)
        if (!fileExists) {
            return SpringResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
        }

        // Read the file as an InputStream
        val inputStream = Files.newInputStream(filePath)
        val resource = InputStreamResource(inputStream)

        val headers = HttpHeaders()
        // TODO: figure out types
        headers.contentType = MediaType.IMAGE_PNG
        headers.set(HttpHeaders.CONTENT_TYPE, "image/webp")
        headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
        headers.set("Content-Disposition", "attachment; filename=${fileEntity.filename}")

        // Return the InputStreamResource
        return org.springframework.http.ResponseEntity(resource, headers, HttpStatus.OK)
    }


    @PostMapping("/files")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, UUID?>> {

        val uuid = UUID.randomUUID();
        val fileName = uuid.toString() + "_" + file.originalFilename
        val filePath = fileDirectory.resolve(fileName)
        val fileExists = Files.exists(filePath)
        // TODO: this currently doesnt work as it uses UUID. Think about UUID collision chance.
        if (fileExists) {
            return ResponseEntity(
                mapOf(
                    "uuid" to null
                ), listOf(ErrorMessage("File already exists")), HttpStatus.BAD_REQUEST.value()
            )
        }

        val fileEntity = FileEntity().also {
            it.uuid = uuid
            it.filename = fileName
            it.originalFilename = file.originalFilename
            it.upload_date = Date()
            it.fileExtension = file.originalFilename?.substringAfterLast('.', "")
            it.fileSize = file.size
        }
        println(fileEntity.toString())

        Files.write(filePath, file.bytes)

        fileRepository.save(fileEntity)

        return ResponseEntity(
            mapOf(
                "uuid" to uuid
            ), HttpStatus.OK.value()
        )

    }

    @DeleteMapping("/files/{token}")
    fun deleteFile(@PathVariable("token") token: UUID): ResponseEntity<Map<String, Boolean>> {

        val fileMeta = fileRepository.findById(token)
        println(fileMeta.toString())
        if (!fileMeta.isPresent) {
            return ResponseEntity(
                mapOf(
                    "deleted" to false
                ), listOf(ErrorMessage("File with the token not found")), HttpStatus.BAD_REQUEST.value()
            )
        }


        fileRepository.deleteById(token)

        return ResponseEntity(
            mapOf(
                "deleted" to true
            ), HttpStatus.OK.value()
        )

    }

//    @PostMapping("/files")

//    @RequestMapping("/files/:file_uuid", method = [RequestMethod.GET]) {
//        entityRepository;
//    }
}