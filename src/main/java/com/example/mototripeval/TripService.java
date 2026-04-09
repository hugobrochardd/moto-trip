

@Service
public class TripService {

    private final TripRepository tripRepo;
    private final UserRepository userRepo;

    public TripService(TripRepository tripRepo, UserRepository userRepo) {
        this.tripRepo = tripRepo;
        this.userRepo = userRepo;
    }

    public Trip createTrip(String name, int max, boolean premiumOnly) {
        if (max <= 0) throw new IllegalArgumentException("Invalid capacity");
        return tripRepo.save(new Trip(name, max, premiumOnly));
    }

    public User createUser(String name, boolean premium) {
        return userRepo.save(new User(name, premium));
    }

    public Trip joinTrip(Long tripId, Long userId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        trip.join(user);
        return tripRepo.save(trip);
    }

    public Trip startTrip(Long id) {
        Trip trip = tripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        trip.start();
        return tripRepo.save(trip);
    }

    public List<Trip> allTrips() {
        return tripRepo.findAll();
    }
}