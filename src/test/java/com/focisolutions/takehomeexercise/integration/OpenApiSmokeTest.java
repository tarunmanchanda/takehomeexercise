package com.focisolutions.takehomeexercise.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:openapismoketestdb;DB_CLOSE_DELAY=-1")
class OpenApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenApplicationIsRunning_whenGetApiDocs_thenReturns200WithOpenApiSpecTest() throws Exception {
        // given
        // (application context already started by @SpringBootTest)

        // when
        // then
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"openapi\"")));
    }

    @Test
    void givenApplicationIsRunning_whenGetSwaggerUi_thenReturns200Test() throws Exception {
        // given
        // (application context already started by @SpringBootTest)

        // when
        // then
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
