package com.focisolutions.takehomeexercise.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:lifecycletestdb;DB_CLOSE_DELAY=-1",
        "spring.security.user.name=" + BasicAuthHeader.USERNAME,
        "spring.security.user.password=" + BasicAuthHeader.PASSWORD
})
class TodoLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenNewTodo_whenFullLifecycleIsExercised_thenAllStateTransitionsPersistCorrectlyTest() throws Exception {
        // given
        final String createBody = """
                {"title":"Buy milk","description":"2 litres","dueDate":"2026-07-10"}""";

        // when: create
        final MvcResult createResult = mockMvc.perform(authorized(post("/api/v1/todos"))
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn();
        final String location = createResult.getResponse().getHeader("Location");
        final JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        final Instant originalUpdatedAt = Instant.parse(created.get("updatedAt").asString());
        assertThat(originalUpdatedAt).isEqualTo(Instant.parse(created.get("createdAt").asString()));

        // then: get returns the created todo
        mockMvc.perform(authorized(get(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.completed").value(false));

        // when: update
        final String updateBody = """
                {"title":"Buy oat milk","description":"2 litres","dueDate":"2026-07-11"}""";
        final MvcResult updateResult = mockMvc.perform(authorized(put(location))
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy oat milk"))
                .andReturn();

        // then: updatedAt was refreshed
        final JsonNode updated = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        assertThat(Instant.parse(updated.get("updatedAt").asString())).isAfter(originalUpdatedAt);

        // when: complete
        mockMvc.perform(authorized(patch(location + "/complete")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        // when: incomplete
        mockMvc.perform(authorized(patch(location + "/incomplete")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));

        // when: delete
        mockMvc.perform(authorized(delete(location))).andExpect(status().isNoContent());

        // then: subsequent get returns 404
        mockMvc.perform(authorized(get(location))).andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder authorized(final MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", BasicAuthHeader.VALUE);
    }
}
