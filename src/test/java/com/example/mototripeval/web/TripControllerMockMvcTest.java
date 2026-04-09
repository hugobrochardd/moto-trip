package com.example.mototripeval.web;

import static com.example.mototripeval.TestAccess.longField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.example.mototripeval.entity.Trip;
import com.example.mototripeval.entity.User;
import com.example.mototripeval.repository.TripRepository;
import com.example.mototripeval.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class TripControllerMockMvcTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void postUsers_shouldCreateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Alice",
                                "premium", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.premium").value(true))
                .andExpect(jsonPath("$.points").value(0));
    }

    @Test
    void postTrips_shouldCreateTrip() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Alps",
                                "maxParticipants", 3,
                                "premiumOnly", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Alps"))
                .andExpect(jsonPath("$.maxParticipants").value(3))
                .andExpect(jsonPath("$.premiumOnly").value(true))
                .andExpect(jsonPath("$.started").value(false))
                .andExpect(jsonPath("$.participants", hasSize(0)));
    }

    @Test
    void postTrips_shouldRejectInvalidCapacity() throws Exception {
        Throwable thrown = catchThrowable(() -> mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Invalid",
                        "maxParticipants", 0,
                        "premiumOnly", false
                )))));

        assertThat(thrown).hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThat(thrown).hasRootCauseMessage("Invalid capacity");
    }

    @Test
    void postJoin_shouldJoinTripAndAwardPoints() throws Exception {
        User user = userRepository.save(new User("Bob", true));
        Trip trip = tripRepository.save(new Trip("Ride", 2, false));

        mockMvc.perform(post("/api/trips/{id}/join", longField(trip, "id"))
                        .param("userId", String.valueOf(longField(user, "id"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(longField(trip, "id")))
                .andExpect(jsonPath("$.participants", hasSize(1)))
                .andExpect(jsonPath("$.participants[0].id").value(longField(user, "id")))
                .andExpect(jsonPath("$.participants[0].name").value("Bob"))
                .andExpect(jsonPath("$.participants[0].points").value(10));
    }

    @Test
    void postJoin_shouldRejectWhenUserDoesNotExist() throws Exception {
        Trip trip = tripRepository.save(new Trip("Ride", 2, false));

        Throwable thrown = catchThrowable(() -> mockMvc.perform(post("/api/trips/{id}/join", longField(trip, "id"))
                .param("userId", "999")));

        assertThat(thrown).hasRootCauseInstanceOf(RuntimeException.class);
        assertThat(thrown).hasRootCauseMessage("User not found");
    }

    @Test
    void postJoin_shouldRejectWhenTripDoesNotExist() throws Exception {
        User user = userRepository.save(new User("Bob", true));

        Throwable thrown = catchThrowable(() -> mockMvc.perform(post("/api/trips/{id}/join", 999)
                .param("userId", String.valueOf(longField(user, "id")))));

        assertThat(thrown).hasRootCauseInstanceOf(RuntimeException.class);
        assertThat(thrown).hasRootCauseMessage("Trip not found");
    }

    @Test
    void postJoin_shouldRejectWhenPremiumIsRequired() throws Exception {
        User user = userRepository.save(new User("Bob", false));
        Trip trip = tripRepository.save(new Trip("VIP Ride", 2, true));

        Throwable thrown = catchThrowable(() -> mockMvc.perform(post("/api/trips/{id}/join", longField(trip, "id"))
                .param("userId", String.valueOf(longField(user, "id")))));

        assertThat(thrown).hasRootCauseInstanceOf(RuntimeException.class);
        assertThat(thrown).hasRootCauseMessage("Premium required");
    }

    @Test
    void postJoin_shouldRejectWhenTripIsFull() throws Exception {
        User firstUser = userRepository.save(new User("Alice", true));
        User secondUser = userRepository.save(new User("Bob", true));
        Trip trip = tripRepository.save(new Trip("Tiny Ride", 1, false));
        trip.join(firstUser);
        tripRepository.save(trip);
        userRepository.save(firstUser);

        Throwable thrown = catchThrowable(() -> mockMvc.perform(post("/api/trips/{id}/join", longField(trip, "id"))
                .param("userId", String.valueOf(longField(secondUser, "id")))));

        assertThat(thrown).hasRootCauseInstanceOf(RuntimeException.class);
        assertThat(thrown).hasRootCauseMessage("Trip full");
    }

    @Test
    void postJoin_shouldRejectWhenTripAlreadyStarted() throws Exception {
        User user = userRepository.save(new User("Alice", true));
        User lateUser = userRepository.save(new User("Bob", true));
        Trip trip = tripRepository.save(new Trip("Started Ride", 2, false));
        trip.join(user);
        trip.start();
        tripRepository.save(trip);
        userRepository.save(user);

        Throwable thrown = catchThrowable(() -> mockMvc.perform(post("/api/trips/{id}/join", longField(trip, "id"))
                .param("userId", String.valueOf(longField(lateUser, "id")))));

        assertThat(thrown).hasRootCauseInstanceOf(RuntimeException.class);
        assertThat(thrown).hasRootCauseMessage("Trip already started");
    }

    @Test
    void postStart_shouldStartTrip() throws Exception {
        User user = userRepository.save(new User("Alice", true));
        Trip trip = tripRepository.save(new Trip("Morning Ride", 2, false));
        trip.join(user);
        tripRepository.save(trip);
        userRepository.save(user);

        mockMvc.perform(post("/api/trips/{id}/start", longField(trip, "id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(longField(trip, "id")))
                .andExpect(jsonPath("$.started").value(true))
                .andExpect(jsonPath("$.participants", hasSize(1)));
    }

    @Test
    void postStart_shouldRejectWhenTripHasNoParticipants() throws Exception {
        Trip trip = tripRepository.save(new Trip("Empty Ride", 2, false));

        Throwable thrown = catchThrowable(() -> mockMvc.perform(post("/api/trips/{id}/start", longField(trip, "id"))));

        assertThat(thrown).hasRootCauseInstanceOf(RuntimeException.class);
        assertThat(thrown).hasRootCauseMessage("No participants");
    }

    @Test
    void postStart_shouldRejectWhenTripDoesNotExist() throws Exception {
        Throwable thrown = catchThrowable(() -> mockMvc.perform(post("/api/trips/{id}/start", 999)));

        assertThat(thrown).hasRootCauseInstanceOf(RuntimeException.class);
        assertThat(thrown).hasRootCauseMessage("Trip not found");
    }

    @Test
    void getTrips_shouldReturnAllExistingTrips() throws Exception {
        tripRepository.save(new Trip("Ride One", 2, false));
        tripRepository.save(new Trip("Ride Two", 3, true));

        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
