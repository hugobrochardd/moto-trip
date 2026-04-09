package com.example.mototripeval.unit;

import static com.example.mototripeval.TestAccess.boolField;
import static com.example.mototripeval.TestAccess.intField;
import static com.example.mototripeval.TestAccess.stringField;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.mototripeval.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UserTest {

    @Test
    void addPoints_shouldAccumulatePoints() {
        User user = new User("Alice", true);

        user.addPoints(10);
        user.addPoints(5);

        assertThat(intField(user, "points")).isEqualTo(15);
    }

    @Test
    void constructor_shouldInitializeNamePremiumAndZeroPoints() {
        User user = new User("Bob", false);

        assertThat(stringField(user, "name")).isEqualTo("Bob");
        assertThat(boolField(user, "premium")).isFalse();
        assertThat(intField(user, "points")).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "true, true",
            "false, false"
    })
    void canJoinPremium_shouldReflectPremiumStatus(boolean premium, boolean expected) {
        User user = new User("Rider", premium);

        assertThat(user.canJoinPremium()).isEqualTo(expected);
    }
}
