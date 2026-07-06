package com.focisolutions.takehomeexercise.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:securitytestdb;DB_CLOSE_DELAY=-1",
        "spring.security.user.name=" + BasicAuthHeader.USERNAME,
        "spring.security.user.password=" + BasicAuthHeader.PASSWORD
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenNoCredentials_whenGetTodos_thenReturns401Test() throws Exception {
        // given
        // (no Authorization header)

        // when
        // then
        mockMvc.perform(get("/api/v1/todos")).andExpect(status().isUnauthorized());
    }

    @Test
    void givenValidCredentials_whenGetTodos_thenReturns200Test() throws Exception {
        // given
        // (BasicAuthHeader matches the pinned spring.security.user.* properties above)

        // when
        // then
        mockMvc.perform(get("/api/v1/todos").header("Authorization", BasicAuthHeader.VALUE))
                .andExpect(status().isOk());
    }

    @Test
    void givenNoCredentials_whenGetSwaggerUi_thenReturns200Test() throws Exception {
        // given
        // (swagger-ui is permitAll())

        // when
        // then
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void givenNoCredentials_whenGetActuatorHealth_thenReturns200Test() throws Exception {
        // given
        // (actuator health is permitAll())

        // when
        // then
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
