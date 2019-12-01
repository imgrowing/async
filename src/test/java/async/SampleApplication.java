package async;

import java.util.concurrent.*;

// https://crondev.blog/2017/01/23/timeouts-with-java-8-completablefuture-youre-probably-doing-it-wrong/
public class SampleApplication {
    public static Integer getValue() {
        System.out.println("I am called");
        // Simulating a long network call of 1 second in the worst case
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 10;
    }

    public static void main(String[] args) {
        ExecutorService executor = new ThreadPoolExecutor(10, 10,
                0L, TimeUnit.MILLISECONDS,
                // This is an unbounded Queue. This should never be used
                // in real life. That is the first step to failure.
                new LinkedBlockingQueue<Runnable>());

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        ScheduledExecutorService schedulerService = Executors.newScheduledThreadPool(10);
        CompletableFuture[] allFutures = new CompletableFuture[10];

        // We want to call the dummy service 10 times
        for (int i = 0; i < 10; ++i) {
            allFutures[i] = CompletableFuture.supplyAsync(() -> {
                // Instead of using CompletableFuture.supplyAsync, directly create a future from the executor
                Future future = executorService.submit(() -> getValue());
                schedulerService.schedule(() -> future.cancel(true), 100, TimeUnit.MILLISECONDS);
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    // pass
                }
                // You can choose to return a dummy value here
                return null;
            });
        }
        // Finally wait for all futures to join
        CompletableFuture.allOf(allFutures).join();
        System.out.println("All futures completed");
        System.out.println(executor.toString());
    }
}