@Entity
public class Trip {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private int maxParticipants;
    private boolean premiumOnly;
    private boolean started;

    @OneToMany
    private List<User> participants = new ArrayList<>();

    public Trip() {}

    public Trip(String name, int maxParticipants, boolean premiumOnly) {
        this.name = name;
        this.maxParticipants = maxParticipants;
        this.premiumOnly = premiumOnly;
        this.started = false;
    }

    public void join(User user) {
        if (started) throw new RuntimeException("Trip already started");

        if (premiumOnly && !user.canJoinPremium()) {
            throw new RuntimeException("Premium required");
        }

        if (participants.size() >= maxParticipants) {
            throw new RuntimeException("Trip full");
        }

        participants.add(user);
        user.addPoints(10);
    }

    public void start() {
        if (participants.isEmpty()) {
            throw new RuntimeException("No participants");
        }
        this.started = true;
    }

    public int remainingPlaces() {
        return maxParticipants - participants.size();
    }
}