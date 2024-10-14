package com.hrblizz.fileapi

import AuthTestSecurityConfig
import TestSecurityConfig
import com.hrblizz.fileapi.data.repository.FileRepository
import com.hrblizz.fileapi.rest.FileAPIConfiguration
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*


const val fileUploadEndpoint = "/api/files"


// generic tests for the API
// here we are testing invalid endpoints, invalid methods and authentication
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("auth-test")
@Import(AuthTestSecurityConfig::class)
class RestTest {
    @Autowired
    private lateinit var mockMvc: MockMvc
    val testingAuth = httpBasic("USER", "PASSWORD1")

    @Test
    fun `should authenticate and return 200 (OK) with valid credentials`() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/files/metas")
                .with(testingAuth)
        ) // Correct username and password
        result.andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `should NOT authenticate and return 401 (Invalid Credentials) with invalid credentials`() {
        val invalidAuth = httpBasic("wrong", "wrong again")

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/files/metas")
                .with(invalidAuth)
        )
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `should return 404 (Not Found) for a bad path`() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/this_doesnt_exist")
                .with(testingAuth)
        )
        result.andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `should return 405 (Method not allowed) for a bad method`() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/files/metas")
                .with(testingAuth)
        )
        result.andExpect(MockMvcResultMatchers.status().isMethodNotAllowed)
        result.andExpect(MockMvcResultMatchers.jsonPath(".errors").isNotEmpty)
    }
}

// a generic file upload method that returns the UUID
private fun uploadTestFile(
    mockMvc: MockMvc,
    filename: String = "test-file.jpg",
    mimeType: String = "image/jpeg",
    content: String = "file-content"
): String {
    val testFile = MockMultipartFile(
        "file",
        filename,
        mimeType,
        content.toByteArray()
    )


    val uploadResult = mockMvc.perform(
        MockMvcRequestBuilders.multipart(fileUploadEndpoint)
            .file(testFile)
    )
        .andExpect(MockMvcResultMatchers.status().isOk)
        .andReturn()

    val jsonResponse = uploadResult.response.contentAsString
    return JsonPath.parse(jsonResponse).read<String>("$.uuid")
}


/**
 * Unit tests for the Restful API
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig::class) // Import the test configuration that skips authentication
class TestGetFile {
    val endpoint = "/api/files/"

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return 400 (Bad Request) for a missing file`() {
        val fileUUID = "none"

        val result = mockMvc.perform(MockMvcRequestBuilders.get("$endpoint$fileUUID"))
        result.andExpect(MockMvcResultMatchers.status().isBadRequest)
        result.andExpect(MockMvcResultMatchers.jsonPath(".errors").isNotEmpty)
    }

}

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig::class)
class TestGetMetadata {
    val endpoint = "/api/files/metas"
    val fileUploadEndpoint = "/api/files"

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var fileRepository: FileRepository

    @BeforeEach
    fun cleanUpDatabase() {
        // Clean the collection before each test to avoid data persistence issues
        fileRepository.deleteAll()
    }

    @Test
    fun `should return files metadata with 200 (OK)`() {
        val result = mockMvc.perform(MockMvcRequestBuilders.get(endpoint))
        result.andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `should return specific files metadata with 200 (OK)`() {
        // uploading some test files...
        val validFile1 =
            MockMultipartFile("file", "test1.jpg", "image/jpeg", "content1".toByteArray()) // file 1: image/jpeg
        val validFile2 =
            MockMultipartFile("file", "test2.png", "image/png", "content2Different".toByteArray()) // file 2: image/png

        mockMvc.perform(MockMvcRequestBuilders.multipart(fileUploadEndpoint).file(validFile1))
            .andExpect(MockMvcResultMatchers.status().isOk)
        mockMvc.perform(MockMvcRequestBuilders.multipart(fileUploadEndpoint).file(validFile2))
            .andExpect(MockMvcResultMatchers.status().isOk)


        // now get the metadata...
        val result = mockMvc.perform(MockMvcRequestBuilders.get(endpoint))

        // check for status and response type
        result.andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray) // Check if the response is an array

            .andExpect(
                MockMvcResultMatchers.jsonPath("$[?(@.originalFilename == 'test1.jpg')].mimeType").value("image/jpeg")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$[?(@.originalFilename == 'test1.jpg')].fileSize").value(8)
            ) // "content1" is 8 bytes
            .andExpect(
                MockMvcResultMatchers.jsonPath("$[?(@.originalFilename == 'test1.jpg')].fileExtension").value("jpg")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.originalFilename == 'test1.jpg')].uuid").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.originalFilename == 'test1.jpg')].uploadDate").isNotEmpty)

            .andExpect(
                MockMvcResultMatchers.jsonPath("$[?(@.originalFilename == 'test2.png')].mimeType").value("image/png")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$[?(@.originalFilename == 'test2.png')].fileSize").value(17)
            ) // "content2Different" is 17 bytes
            .andExpect(
                MockMvcResultMatchers.jsonPath("$[?(@.originalFilename == 'test2.png')].fileExtension").value("png")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.originalFilename == 'test2.png')].uuid").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.originalFilename == 'test2.png')].uploadDate").isNotEmpty)
    }

}

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig::class)
class TestUploadFile {
    val endpoint = "/api/files"

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `file upload with invalid mime type should return 400 (Bad Request)`() {
        val invalidFile = MockMultipartFile("file", "test.txt", "text/plain", "some content".toByteArray())

        println("endpoint: $endpoint")
        val result = mockMvc.perform(MockMvcRequestBuilders.multipart(endpoint).file(invalidFile))

        result.andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid file type"))
    }

    @Test
    fun `file upload with valid mime type and valid size should return 200 (Ok) and a token`() {
        val validFile = MockMultipartFile("file", "test.jpg", "image/jpeg", "some content".toByteArray())

        val result = mockMvc.perform(MockMvcRequestBuilders.multipart(endpoint).file(validFile))

        result.andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uuid").isNotEmpty)

    }

    @Test
    fun `test upload with valid mime type but exceeding size limit`() {
        val largeFile = MockMultipartFile("file", "largeFile.jpg", "image/jpeg", ByteArray(12 * 1024 * 1024)) // 12 MB

        val result = mockMvc.perform(
            MockMvcRequestBuilders.multipart(endpoint)
                .file(largeFile)
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )

        result.andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("File exceeds maximum file size of 5242880"))
    }

    @Test
    fun `test upload with valid mime type but invalid size limit (too small)`() {
        val smallFile = MockMultipartFile("file", "smallFile.jpg", "image/jpeg", ByteArray(1)) // 1 byte

        val result = mockMvc.perform(
            MockMvcRequestBuilders.multipart(endpoint)
                .file(smallFile)
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )

        result.andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uuid").isNotEmpty)
    }

}

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig::class)
class TestFetchFileMetas {

    val endpoint = "/api/files/metas"

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `fetch file metadata for existing tokens should return 200 (OK)`() {
        val uploadedFileUuid = uploadTestFile(mockMvc, "test-file.jpg", "image/jpeg", "file-content")

        // Create request body with the uploaded file UUID
        val requestBody = """
            {
                "tokens": ["$uploadedFileUuid"]
            }
        """.trimIndent()

        // Fetch metadata for the uploaded file
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        result.andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].uuid").value(uploadedFileUuid))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].originalFilename").value("test-file.jpg"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].mimeType").value("image/jpeg"))
    }

    @Test
    fun `fetch file metadata for non-existing tokens should return empty array with 200 (OK)`() {
        // Create request body with a non-existing UUID token
        val requestBody = """
            {
                "tokens": ["${UUID.randomUUID()}"]
            }
        """.trimIndent()

        // Fetch metadata for the non-existing file
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        result.andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty)
    }

    @Test
    fun `fetch file metadata for mixed valid and invalid tokens should return 200 (OK)`() {
        val filename = "valid_file"
        val validFileUuid = uploadTestFile(mockMvc, filename)
        val invalidToken = UUID.randomUUID().toString() // Non-existing UUID

        // Create request body with a mix of valid and invalid tokens
        val requestBody = """
            {
                "tokens": ["$validFileUuid", "$invalidToken"]
            }
        """.trimIndent()

        // Fetch metadata for the mixed tokens
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        result.andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].uuid").value(validFileUuid))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].originalFilename").value(filename))
    }

}

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig::class)
class TestDeleteFile {
    val deleteFileEndpoint = "/api/files/"

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `delete file with invalid token should return 400 (Bad Request)`() {
        // Generate a random UUID token
        val randomToken = UUID.randomUUID().toString()

        val result = mockMvc.perform(
            MockMvcRequestBuilders.delete("$deleteFileEndpoint$randomToken")
        )

        result.andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("File with the token not found"))
    }

    @Test
    fun `should upload, delete, and verify file is deleted`() {
        val uploadedFileUuid = uploadTestFile(mockMvc, "test-file.jpg", "image/jpeg", "file-content")

        mockMvc.perform(MockMvcRequestBuilders.delete("$deleteFileEndpoint$uploadedFileUuid"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.deleted").value(true))

        mockMvc.perform(MockMvcRequestBuilders.delete("$deleteFileEndpoint$uploadedFileUuid"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("File with the token not found"))
    }

}

//@SpringBootTest
//@EnableConfigurationProperties(FileAPIConfiguration::class)
//class FileCleanupTest {
//
//    companion object {
//        private lateinit var testDirectoryPath: Path
//
//        @JvmStatic
//        @BeforeAll
//        fun init(@Autowired fileAPIConfiguration: FileAPIConfiguration) {
//            // Set the test directory from the configuration
//            testDirectoryPath = Paths.get(fileAPIConfiguration.fileDirectory)
//        }
//
//
//        @JvmStatic
//        @AfterAll
//        fun tearDown() {
//            deleteAllFilesInDirectory(testDirectoryPath.toString())
//        }
//
//        private fun deleteAllFilesInDirectory(directory: String) {
//            val path: Path = Paths.get(directory)
//            if (Files.exists(path) && Files.isDirectory(path)) {
//                Files.list(path).forEach { file ->
//                    Files.deleteIfExists(file)  // Deletes the file if it exists
//                }
//            }
//        }
//    }