import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

public class FlashSaleInventoryManager {

    // O(1) product lookup
    private final ConcurrentHashMap<String, AtomicInteger> stockMap = new ConcurrentHashMap<>();

    // FIFO waiting list per product
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> waitingList = new ConcurrentHashMap<>();

    // Add product
    public void addProduct(String productId, int initialStock) {
        stockMap.put(productId, new AtomicInteger(initialStock));
        waitingList.put(productId, new ConcurrentLinkedQueue<>());
    }

    // Instant stock check
    public int checkStock(String productId) {
        AtomicInteger stock = stockMap.get(productId);
        return stock != null ? stock.get() : 0;
    }

    // Thread-safe purchase
    public String purchaseItem(String productId, long userId) {

        AtomicInteger stock = stockMap.get(productId);

        if (stock == null) {
            return "Product not found";
        }

        while (true) {
            int currentStock = stock.get();

            if (currentStock <= 0) {
                ConcurrentLinkedQueue<Long> queue = waitingList.get(productId);
                queue.add(userId);
                return "Added to waiting list, position #" + queue.size();
            }

            // Atomic decrement using CAS
            if (stock.compareAndSet(currentStock, currentStock - 1)) {
                return "Success, " + (currentStock - 1) + " units remaining";
            }

            // Retry if another thread updated value
        }
    }

    // ============================
    // LOAD TEST (Simulate 50,000 users)
    // ============================

    public static void main(String[] args) throws InterruptedException {

        FlashSaleInventoryManager manager = new FlashSaleInventoryManager();

        String productId = "IPHONE15_256GB";
        manager.addProduct(productId, 100);

        System.out.println("Initial Stock: " + manager.checkStock(productId));

        int totalUsers = 50000;

        ExecutorService executor = Executors.newFixedThreadPool(200);

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= totalUsers; i++) {
            long userId = i;

            executor.execute(() -> {
                String result = manager.purchaseItem(productId, userId);
                // Comment below line if you want faster performance
                // System.out.println("User " + userId + ": " + result);
            });
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        System.out.println("Final Stock: " + manager.checkStock(productId));
        System.out.println("Waiting List Size: " +
                manager.waitingList.get(productId).size());
        System.out.println("Execution Time: " + (endTime - startTime) + " ms");
    }
}
