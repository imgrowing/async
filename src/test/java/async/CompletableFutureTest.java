package async;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.*;
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

    @Test
    public void thenAccept() throws ExecutionException, InterruptedException {
        Runnable runnable = () -> log.info("runAsync()");
        Consumer<Void> voidConsumer = (v) -> log.info("result: " + v);
        CompletableFuture.runAsync(runnable).thenAccept(voidConsumer);

        Supplier<String> supplier = () -> {
            log.info("supplyAsync()");
            return "test";
        };
        Consumer<String> stringConsumer = (s) -> log.info("result: " + s);
        CompletableFuture.supplyAsync(supplier).thenAccept(stringConsumer);

        Thread.sleep(200);

        /*
        runAsync(runnable)과 supplyAsync(supplier)는 CompletableFuture 인스턴스를 반환한다.
        completableFuture.thenAccept(Consumer<T>)는 CompletableFuture의 completion 결과를 파라미터로 consumer에게 전달한다.
        thenAccept(consumer)는 completableFuture에 consumer라는 callback을 등록하는 것과 같다.
        위의 두 async 호출은 코드 순서대로 실행되는 것이 보장되지 않는다.

        [Result]
        01:11:00.613 [ForkJoinPool.commonPool-worker-2] INFO async.CompletableFutureTest - supplyAsync()
        01:11:00.613 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - runAsync()
        01:11:00.616 [ForkJoinPool.commonPool-worker-2] INFO async.CompletableFutureTest - result: test
        01:11:00.616 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - result: null
        */
    }
}
