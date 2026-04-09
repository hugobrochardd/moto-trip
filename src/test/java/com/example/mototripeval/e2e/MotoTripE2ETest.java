package com.example.mototripeval.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import com.example.mototripeval.repository.TripRepository;
import com.example.mototripeval.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MotoTripE2ETest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void cleanDatabase() {
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUserCreateTripJoinStartAndExposeStartedTrip() throws Exception {
        HttpResponse<String> userResponse = postJson(
                "/api/users",
                Map.of("name", "Alice", "premium", true)
        );
        HttpResponse<String> tripResponse = postJson(
                "/api/trips",
                Map.of("name", "Alps Ride", "maxParticipants", 2, "premiumOnly", false)
        );

        Map<String, Object> userBody = objectMapper.readValue(userResponse.body(), new TypeReference<>() {
        });
        Map<String, Object> tripBody = objectMapper.readValue(tripResponse.body(), new TypeReference<>() {
        });
        Long userId = asLong(userBody.get("id"));
        Long tripId = asLong(tripBody.get("id"));

        HttpResponse<String> joinResponse = postWithoutBody(
                "/api/trips/" + tripId + "/join?userId=" + userId
        );
        HttpResponse<String> startResponse = postWithoutBody(
                "/api/trips/" + tripId + "/start"
        );
        HttpResponse<String> tripsResponse = get(
                "/api/trips"
        );
        List<Map<String, Object>> tripsBody = objectMapper.readValue(tripsResponse.body(), new TypeReference<>() {
        });

        assertThat(userResponse.statusCode()).isEqualTo(200);
        assertThat(tripResponse.statusCode()).isEqualTo(200);
        assertThat(joinResponse.statusCode()).isEqualTo(200);
        assertThat(startResponse.statusCode()).isEqualTo(200);
        assertThat(tripsResponse.statusCode()).isEqualTo(200);

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

    private HttpResponse<String> postJson(String path, Map<String, Object> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithoutBody(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
