import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UsernameAvailabilitySystem {

    // Stores username -> userId (O(1) lookup)
    private final ConcurrentHashMap<String, Integer> userDatabase;

    // Stores username -> attempt frequency
    private final ConcurrentHashMap<String, AtomicInteger> attemptFrequency;

    public UsernameAvailabilitySystem() {
        this.userDatabase = new ConcurrentHashMap<>();
        this.attemptFrequency = new ConcurrentHashMap<>();
    }

    // ----------------------------
    // Register Existing User
    // ----------------------------
    public void registerUser(String username, int userId) {
        validateUsername(username);
        userDatabase.put(username.toLowerCase(), userId);
    }

    // ----------------------------
    // O(1) Availability Check
    // Thread-safe for high concurrency
    // ----------------------------
    public boolean checkAvailability(String username) {
        validateUsername(username);
        username = username.toLowerCase();

        // Track attempt frequency safely
        attemptFrequency
                .computeIfAbsent(username, k -> new AtomicInteger(0))
                .incrementAndGet();

        return !userDatabase.containsKey(username);
    }

    // ----------------------------
    // Suggest Similar Available Usernames
    // ----------------------------
    public List<String> suggestAlternatives(String username) {
        validateUsername(username);
        username = username.toLowerCase();

        List<String> suggestions = new ArrayList<>();

        // Strategy 1: Append numbers
        for (int i = 1; i <= 5; i++) {
            String suggestion = username + i;
            if (!userDatabase.containsKey(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        // Strategy 2: Replace "_" with "."
        if (username.contains("_")) {
            String modified = username.replace("_", ".");
            if (!userDatabase.containsKey(modified)) {
                suggestions.add(modified);
            }
        }

        // Strategy 3: Append random 3-digit number
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            String suggestion = username + (100 + random.nextInt(900));
            if (!userDatabase.containsKey(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    // ----------------------------
    // Get Most Attempted Username
    // ----------------------------
    public String getMostAttempted() {
        return attemptFrequency.entrySet()
                .stream()
                .max(Comparator.comparingInt(e -> e.getValue().get()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // ----------------------------
    // Optional: Get Attempt Count
    // ----------------------------
    public int getAttemptCount(String username) {
        username = username.toLowerCase();
        return attemptFrequency.getOrDefault(username, new AtomicInteger(0)).get();
    }

    // ----------------------------
    // Username Validation
    // ----------------------------
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        if (!username.matches("^[a-zA-Z0-9._]{3,20}$")) {
            throw new IllegalArgumentException(
                    "Username must be 3-20 characters and contain only letters, numbers, . or _"
            );
        }
    }

    // ----------------------------
    // Simulate 1000 Concurrent Checks
    // ----------------------------
    public static void simulateConcurrency(UsernameAvailabilitySystem system)
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(50);

        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> system.checkAvailability("admin"));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    // ----------------------------
    // Main Method (Testing)
    // ----------------------------
    public static void main(String[] args) throws InterruptedException {

        UsernameAvailabilitySystem system = new UsernameAvailabilitySystem();

        // Simulate 10M users (small sample here)
        system.registerUser("john_doe", 1);
        system.registerUser("admin", 2);
        system.registerUser("elonmusk", 3);

        // Sample Checks
        System.out.println(system.checkAvailability("john_doe"));   // false
        System.out.println(system.checkAvailability("jane_smith")); // true

        // Suggestions
        System.out.println(system.suggestAlternatives("john_doe"));

        // Simulate heavy concurrent traffic
        simulateConcurrency(system);

        // Popularity tracking
        System.out.println("Most Attempted: " + system.getMostAttempted());
        System.out.println("Admin Attempts: " + system.getAttemptCount("admin"));
    }
}
