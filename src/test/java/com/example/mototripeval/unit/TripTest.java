package com.example.mototripeval.unit;

import static com.example.mototripeval.TestAccess.boolField;
import static com.example.mototripeval.TestAccess.field;
import static com.example.mototripeval.TestAccess.intField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.example.mototripeval.entity.Trip;
import com.example.mototripeval.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TripTest {

    @Test
    void join_shouldAddParticipantGrantPointsAndUpdateRemainingPlaces() {
        Trip trip = new Trip("Alps", 2, false);
        User user = new User("Alice", false);

        trip.join(user);

        List<User> participants = field(trip, "participants");
        assertThat(participants).containsExactly(user);
        assertThat(intField(user, "points")).isEqualTo(10);
        assertThat(trip.remainingPlaces()).isEqualTo(1);
    }

    @Test
    void join_shouldRejectWhenTripAlreadyStarted() {
        Trip trip = new Trip("Alps", 2, false);
        User user = new User("Alice", false);
        ReflectionTestUtils.setField(trip, "started", true);

        assertThatThrownBy(() -> trip.join(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Trip already started");
    }

    @Test
    void join_shouldRejectNonPremiumUserForPremiumTrip() {
        Trip trip = new Trip("VIP Ride", 2, true);
        User user = new User("Bob", false);

        assertThatThrownBy(() -> trip.join(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Premium required");
    }

    @Test
    void join_shouldAcceptPremiumUserForPremiumTrip() {
        Trip trip = new Trip("VIP Ride", 2, true);
        User user = new User("Bob", true);

        trip.join(user);

        List<User> participants = field(trip, "participants");
        assertThat(participants).containsExactly(user);
        assertThat(intField(user, "points")).isEqualTo(10);
        assertThat(trip.remainingPlaces()).isEqualTo(1);
    }

    @Test
    void join_shouldRejectWhenTripIsFull() {
        Trip trip = new Trip("Small Ride", 1, false);
        trip.join(new User("Alice", false));

        assertThatThrownBy(() -> trip.join(new User("Bob", true)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Trip full");
    }

    @Test
    void start_shouldMarkTripAsStartedWhenThereIsAtLeastOneParticipant() {
        Trip trip = new Trip("Morning Ride", 2, false);
        trip.join(new User("Alice", false));

        trip.start();

        assertThat(boolField(trip, "started")).isTrue();
    }

    @Test
    void start_shouldRejectWhenThereAreNoParticipants() {
        Trip trip = new Trip("Empty Ride", 2, false);

        assertThatThrownBy(trip::start)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No participants");
    }
}
