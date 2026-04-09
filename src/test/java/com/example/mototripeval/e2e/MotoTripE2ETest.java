package com.example.mototripeval.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.example.mototripeval.repository.TripRepository;
import com.example.mototripeval.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class MotoTripE2ETest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUserCreateTripJoinStartAndExposeStartedTrip() throws Exception {
        MvcResult userResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Alice",
                                "premium", true
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult tripResponse = mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Alps Ride",
                                "maxParticipants", 2,
                                "premiumOnly", false
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> userBody = objectMapper.readValue(userResponse.getResponse().getContentAsString(), new TypeReference<>() {
        });
        Map<String, Object> tripBody = objectMapper.readValue(tripResponse.getResponse().getContentAsString(), new TypeReference<>() {
        });
        Long userId = asLong(userBody.get("id"));
        Long tripId = asLong(tripBody.get("id"));

        MvcResult joinResponse = mockMvc.perform(post("/api/trips/{id}/join", tripId)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult startResponse = mockMvc.perform(post("/api/trips/{id}/start", tripId))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult tripsResponse = mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> tripsBody = objectMapper.readValue(tripsResponse.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(userResponse.getResponse().getStatus()).isEqualTo(200);
        assertThat(tripResponse.getResponse().getStatus()).isEqualTo(200);
        assertThat(joinResponse.getResponse().getStatus()).isEqualTo(200);
        assertThat(startResponse.getResponse().getStatus()).isEqualTo(200);
        assertThat(tripsResponse.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> startedTrip = tripsBody.get(0);
        List<Map<String, Object>> participants = (List<Map<String, Object>>) startedTrip.get("participants");

        assertThat(tripsBody).hasSize(1);
        assertThat(startedTrip.get("name")).isEqualTo("Alps Ride");
        assertThat(startedTrip.get("started")).isEqualTo(true);
        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).get("id")).isEqualTo(userId.intValue());
        assertThat(participants.get(0).get("points")).isEqualTo(10);
    }

    private Long asLong(Object value) {
        return ((Number) value).longValue();
    }
}
