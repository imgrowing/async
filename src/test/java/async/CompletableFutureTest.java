package async;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class CompletableFutureTest {

    @Test
    public void sample() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        executor.submit(() -> {
            log.info("start");
            Thread.sleep(500);
            completableFuture.complete("completed");
            return null;
        });

        String s = completableFuture.get(); // blocking 발생
        log.info(s);

        Thread.sleep(500);
    }

    @Test
    public void runAsync() throws ExecutionException, InterruptedException {
        Runnable runnable = () -> {
            log.info("runAsync()...");
        };
        Void aVoid = CompletableFuture.runAsync(runnable).get();
        /*
        runAsync(Runnable)은 파라미터가 Runnable 이므로 반환값이 없다(Void).
        ForkJoinPool.commonPool() 을 사용한다.

        [결과]
        00:24:48.436 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureEx - runAsync()...
         */
    }

    @Test
    public void supplyAsync() throws ExecutionException, InterruptedException {
        Supplier<String> supplier = () -> {
            log.info("supplyAsync()...");
            return "supplyAsync()...";
        };
        log.info("result: " + CompletableFuture.supplyAsync(supplier).get());
        /*
        supplyAsync(Supplier<U>)는 파라미터가 Supplier<U> 이므로 반환값은 <U>이다.
        ForkJoinPool.commonPool() 을 사용한다.

        [Result]
        00:31:50.711 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureEx - supplyAsync()...
        00:31:50.713 [main] INFO async.CompletableFutureEx - result: supplyAsync()...
         */
    }
}
