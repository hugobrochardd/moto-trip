package com.example.mototripeval.unit;

import static com.example.mototripeval.TestAccess.boolField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static com.example.mototripeval.TestAccess.field;
import static com.example.mototripeval.TestAccess.intField;
import static com.example.mototripeval.TestAccess.stringField;

import java.util.List;
import java.util.Optional;

import com.example.mototripeval.entity.Trip;
import com.example.mototripeval.entity.User;
import com.example.mototripeval.repository.TripRepository;
import com.example.mototripeval.repository.UserRepository;
import com.example.mototripeval.service.TripService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TripService tripService;

    @Test
    void createTrip_shouldSaveTripWhenCapacityIsValid() {
        given(tripRepository.save(any(Trip.class))).willAnswer(invocation -> invocation.getArgument(0));

        Trip createdTrip = tripService.createTrip("Alps", 3, true);

        ArgumentCaptor<Trip> captor = ArgumentCaptor.forClass(Trip.class);
        then(tripRepository).should().save(captor.capture());
        Trip savedTrip = captor.getValue();

        assertThat(stringField(savedTrip, "name")).isEqualTo("Alps");
        assertThat(intField(savedTrip, "maxParticipants")).isEqualTo(3);
        assertThat(boolField(savedTrip, "premiumOnly")).isTrue();
        assertThat(boolField(savedTrip, "started")).isFalse();
        assertThat(createdTrip).isSameAs(savedTrip);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5})
    void createTrip_shouldRejectInvalidCapacity(int invalidCapacity) {
        assertThatThrownBy(() -> tripService.createTrip("Invalid", invalidCapacity, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid capacity");

        then(tripRepository).should(never()).save(any());
    }

    @Test
    void joinTrip_shouldJoinAndSaveTripWhenTripAndUserExist() {
        Trip trip = new Trip("Alps", 2, false);
        User user = new User("Alice", true);
        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(tripRepository.save(trip)).willReturn(trip);

        Trip updatedTrip = tripService.joinTrip(1L, 2L);

        List<User> participants = field(trip, "participants");
        assertThat(updatedTrip).isSameAs(trip);
        assertThat(participants).containsExactly(user);
        assertThat(intField(user, "points")).isEqualTo(10);
        then(tripRepository).should().save(trip);
    }

    @Test
    void joinTrip_shouldThrowWhenTripDoesNotExist() {
        given(tripRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.joinTrip(99L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Trip not found");

        then(userRepository).should(never()).findById(any());
        then(tripRepository).should(never()).save(any());
    }

    @Test
    void joinTrip_shouldThrowWhenUserDoesNotExist() {
        Trip trip = new Trip("Alps", 2, false);
        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(userRepository.findById(42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.joinTrip(1L, 42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        then(tripRepository).should(never()).save(any());
    }

    @Test
    void startTrip_shouldStartAndSaveTrip() {
        Trip trip = new Trip("Morning Ride", 2, false);
        trip.join(new User("Alice", false));
        given(tripRepository.findById(5L)).willReturn(Optional.of(trip));
        given(tripRepository.save(trip)).willReturn(trip);

        Trip startedTrip = tripService.startTrip(5L);

        assertThat(startedTrip).isSameAs(trip);
        assertThat(boolField(trip, "started")).isTrue();
        then(tripRepository).should().save(trip);
    }
}
