package async;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class CompletableFutureTest {

    Runnable RUNNABLE = () -> log.info("runnable ran");

    Supplier<String> STRING_SUPPLIER = () -> {
        log.info("supplier supplied (output) : test");
        return "test";
    };

    Function<String, String> STRING_FUNCTION = (s) -> {
        log.info("function applied (input / output) : " + s + "/" + s);
        return s;
    };

    Consumer<String> STRING_CONSUMER = (s) -> log.info("consumer accepted (input) : " + s);
    Consumer<Void> VOID_CONSUMER = (v) -> log.info("consumer accepted (input) : " + v);

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
        Void aVoid = CompletableFuture
                .runAsync(RUNNABLE)
                .get();
        /*
        runAsync(Runnable)은 파라미터가 Runnable 이므로 반환값이 없다(Void).
        ForkJoinPool.commonPool() 을 사용한다.

        [결과]
        00:09:27.623 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - runnable ran
         */
    }

    @Test
    public void supplyAsync() throws ExecutionException, InterruptedException {
        String result = CompletableFuture
                .supplyAsync(STRING_SUPPLIER)
                .get();
        log.info("result: " + result);
        /*
        supplyAsync(Supplier<U>)는 파라미터가 Supplier<U> 이므로 반환값은 <U>이다.
        ForkJoinPool.commonPool() 을 사용한다.

        [Result]
        00:10:08.205 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - supplier supplied (output) : test
        00:10:08.207 [main] INFO async.CompletableFutureTest - result: test
         */
    }

    @Test
    public void thenAccept() throws InterruptedException {
        CompletableFuture
                .runAsync(RUNNABLE)
                .thenAccept(VOID_CONSUMER);

        CompletableFuture
                .supplyAsync(STRING_SUPPLIER)
                .thenAccept(STRING_CONSUMER);

        Thread.sleep(200);

        /*
        runAsync(runnable)과 supplyAsync(supplier)는 CompletableFuture 인스턴스를 반환한다.
        completableFuture.thenAccept(Consumer<T>)는 CompletableFuture의 completion 결과를 파라미터로 consumer에게 전달한다.
        thenAccept(consumer)는 completableFuture에 consumer라는 callback을 등록하는 것과 같다.
        위의 두 ...Async() 호출은 코드 순서대로 실행되는 것이 보장되지 않는다.

        [Result]
        00:13:34.840 [ForkJoinPool.commonPool-worker-2] INFO async.CompletableFutureTest - supplier supplied (output) : test
        00:13:34.840 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - runnable ran
        00:13:34.842 [ForkJoinPool.commonPool-worker-2] INFO async.CompletableFutureTest - consumer accepted (input) : test
        00:13:34.842 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - consumer accepted (input) : null
        */
    }

    @Test
    public void thenAccept_중첩() throws InterruptedException {
        CompletableFuture
                .supplyAsync(STRING_SUPPLIER)
                .thenAccept(STRING_CONSUMER)
                .thenAccept(VOID_CONSUMER);

        Thread.sleep(200);
        /*
        thenAccept(consumer)의 리턴 타입이 CompletableFuture<Void> 이기 때문에 thenAccept(consumer)를 연속해서 사용할 수 있다.
        하지만 두 번째 연결된 consumer는 앞 단계의 결과를 전달받을 수 없다(Void를 전달받음).
        supplyAsync(supplier) 단계의 결과를 전달받으려면 thenApply(Function<I, O>)를 사용해야 한다.
        thenAccept(consumer)는 앞 단계에서 사용했던 스레드에서 수행된다.
        아래의 결과를 살펴보면 thenAccept(..)는 supplyAsync(Supplier)가 실행된
        ForkJoinPool.commonPool() 스레드 상에서 모두 실행되었다.

        [Result]
        00:14:47.303 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - supplier supplied (output) : test
        00:14:47.306 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - consumer accepted (input) : test
        00:14:47.306 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - consumer accepted (input) : null
         */
    }

    @Test
    public void thenApply_중첩() throws InterruptedException {
        CompletableFuture
                .supplyAsync(STRING_SUPPLIER)
                .thenApply(STRING_FUNCTION)
                .thenAccept(STRING_CONSUMER);

        Thread.sleep(200);
        /*
        supplyAsync(supplier<S>)의 결과인 <S>를 계속 전달하려면,
        thenApply(function<I, O>)를 호출해서 결과를 다음 단계의 .thenXxx()로 넘겨야 한다.

        [Result]
        00:19:06.188 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - supplier supplied (output) : test
        00:19:06.191 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - function applied (input / output) : test/test
        00:19:06.191 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - consumer accepted (input) : test
         */
    }
}
