package com.example.mototripeval.integration;

import static com.example.mototripeval.TestAccess.boolField;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.mototripeval.entity.Trip;
import com.example.mototripeval.entity.User;
import com.example.mototripeval.repository.TripRepository;
import com.example.mototripeval.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static com.example.mototripeval.TestAccess.longField;

@SpringBootTest
class TripPersistenceIntegrationTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void saveUserAndTrip_shouldPersistCoreFields() {
        User savedUser = userRepository.saveAndFlush(new User("Alice", true));
        Trip savedTrip = tripRepository.saveAndFlush(new Trip("Alps", 3, true));

        assertThat(savedUser).isNotNull();
        assertThat(savedTrip).isNotNull();
        assertThat(tripRepository.findAll()).hasSize(1);
        assertThat(userRepository.findAll()).hasSize(1);
        assertThat(boolField(savedTrip, "started")).isFalse();
        assertThat(boolField(savedTrip, "premiumOnly")).isTrue();
    }

    @Test
    void join_shouldPersistTripParticipantRelationAndUserPoints() {
        User user = userRepository.saveAndFlush(new User("Bob", true));
        Trip trip = tripRepository.saveAndFlush(new Trip("Mountain Ride", 2, false));

        trip.join(user);
        tripRepository.saveAndFlush(trip);
        userRepository.saveAndFlush(user);

        Integer relationCount = jdbcTemplate.queryForObject(
                "select count(*) from trip_participants where trip_id = ? and participants_id = ?",
                Integer.class,
                longField(trip, "id"),
                longField(user, "id")
        );
        Integer points = jdbcTemplate.queryForObject(
                "select points from user where id = ?",
                Integer.class,
                longField(user, "id")
        );

        assertThat(relationCount).isEqualTo(1);
        assertThat(points).isEqualTo(10);
    }

    @Test
    void start_shouldPersistStartedState() {
        User user = userRepository.saveAndFlush(new User("Cara", false));
        Trip trip = tripRepository.saveAndFlush(new Trip("Sunrise", 2, false));
        trip.join(user);
        trip.start();
        tripRepository.saveAndFlush(trip);
        userRepository.saveAndFlush(user);

        Boolean started = jdbcTemplate.queryForObject(
                "select started from trip where id = ?",
                Boolean.class,
                longField(trip, "id")
        );

        assertThat(started).isTrue();
    }
}
