package com.example.mototripeval.unit;

import static com.example.mototripeval.TestAccess.field;
import static com.example.mototripeval.TestAccess.intField;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.example.mototripeval.entity.Trip;
import com.example.mototripeval.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TripConcurrencyTest {

    @Test
    void join_shouldHandleTwoConcurrentUsersWhenCapacityAllows() throws Exception {
        Trip trip = new Trip("Concurrent Ride", 2, false);
        CoordinatedParticipantsList participants = new CoordinatedParticipantsList(2);
        ReflectionTestUtils.setField(trip, "participants", participants);

        User firstUser = new User("Alice", true);
        User secondUser = new User("Bob", true);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstJoin = executor.submit(() -> trip.join(firstUser));
            Future<?> secondJoin = executor.submit(() -> trip.join(secondUser));

            await(firstJoin);
            await(secondJoin);

            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        List<User> savedParticipants = field(trip, "participants");
        assertThat(savedParticipants).containsExactlyInAnyOrder(firstUser, secondUser);
        assertThat(intField(firstUser, "points")).isEqualTo(10);
        assertThat(intField(secondUser, "points")).isEqualTo(10);
        assertThat(trip.remainingPlaces()).isZero();
    }

    private void await(Future<?> future) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new AssertionError("Concurrent task failed", e.getCause());
        }
    }

    private static final class CoordinatedParticipantsList extends AbstractList<User> {
        private final CopyOnWriteArrayList<User> delegate = new CopyOnWriteArrayList<>();
        private final CountDownLatch sizeBarrier;

        private CoordinatedParticipantsList(int concurrentCalls) {
            this.sizeBarrier = new CountDownLatch(concurrentCalls);
        }

        @Override
        public User get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            sizeBarrier.countDown();
            try {
                if (!sizeBarrier.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out while synchronizing concurrent joins");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while synchronizing concurrent joins", e);
            }
            return delegate.size();
        }

        @Override
        public boolean add(User user) {
            return delegate.add(user);
        }
    }
}
