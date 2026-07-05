package com.focisolutions.takehomeexercise.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:lifecycletestdb;DB_CLOSE_DELAY=-1")
class TodoLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenNewTodo_whenFullLifecycleIsExercised_thenAllStateTransitionsPersistCorrectlyTest() throws Exception {
        // given
        final String createBody = """
                {"title":"Buy milk","description":"2 litres","dueDate":"2026-07-10"}""";

        // when: create
        final MvcResult createResult = mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn();
        final String location = createResult.getResponse().getHeader("Location");

        // then: get returns the created todo
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.completed").value(false));

        // when: update
        final String updateBody = """
                {"title":"Buy oat milk","description":"2 litres","dueDate":"2026-07-11"}""";
        mockMvc.perform(put(location)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy oat milk"));

        // when: complete
        mockMvc.perform(patch(location + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        // when: incomplete
        mockMvc.perform(patch(location + "/incomplete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));

        // when: delete
        mockMvc.perform(delete(location)).andExpect(status().isNoContent());

        // then: subsequent get returns 404
        mockMvc.perform(get(location)).andExpect(status().isNotFound());
    }
}
