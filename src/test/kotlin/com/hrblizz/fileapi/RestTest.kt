package com.hrblizz.fileapi

import TestSecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * Unit tests for the Restful API
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig::class) // Import the test configuration that skips authentication
class RestTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return 400 (Bad Request) for a missing file`() {
        val fileUUID = "none"

        val result = mockMvc.perform(MockMvcRequestBuilders.get("/api/files/$fileUUID"))
        result.andExpect(MockMvcResultMatchers.status().isBadRequest)
        result.andExpect(MockMvcResultMatchers.jsonPath(".errors").isNotEmpty)
    }

}
