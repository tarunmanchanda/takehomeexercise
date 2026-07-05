package com.focisolutions.takehomeexercise.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:filterandsorttestdb;DB_CLOSE_DELAY=-1")
class TodoFilterAndSortIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenTodosWithVariedStateAndDueDates_whenFilteringAndSorting_thenResultsMatchEachCriteriaTest() throws Exception {
        // given
        final String overdueDueDate = LocalDate.now().minusDays(1).toString();
        mockMvc.perform(post("/todos").contentType("application/json")
                .content("{\"title\":\"Charlie\"}")).andExpect(status().isCreated());
        final String alphaLocation = mockMvc.perform(post("/todos").contentType("application/json")
                        .content("{\"title\":\"Alpha\",\"dueDate\":\"" + overdueDueDate + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        final String bravoLocation = mockMvc.perform(post("/todos").contentType("application/json")
                        .content("{\"title\":\"Bravo\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        mockMvc.perform(patch(bravoLocation + "/complete")).andExpect(status().isOk());

        // when: filter by OVERDUE
        // then: only Alpha (incomplete, dueDate in the past)
        mockMvc.perform(get("/todos").param("status", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Alpha"));

        // when: filter by COMPLETED
        // then: only Bravo
        mockMvc.perform(get("/todos").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Bravo"));

        // when: filter by INCOMPLETE
        // then: Alpha and Charlie, not Bravo
        mockMvc.perform(get("/todos").param("status", "INCOMPLETE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // when: sort ALL by title ascending
        // then: Alpha, Bravo, Charlie in order
        mockMvc.perform(get("/todos").param("sortBy", "TITLE").param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Alpha"))
                .andExpect(jsonPath("$[1].title").value("Bravo"))
                .andExpect(jsonPath("$[2].title").value("Charlie"));
    }
}
