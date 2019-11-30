package async;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class CompletableFutureTest {

    Runnable RUNNABLE = () -> log.info("runnable ran");

    Supplier<String> STRING_SUPPLIER1 = () -> {
        String output = "test";
        log.info("supplier supplied (output) : " + output);
        return output;
    };

    Supplier<String> STRING_SUPPLIER2 = () -> {
        String output = " !!!";
        log.info("another supplier supplied (output) : " + output);
        return output;
    };

    Function<String, String> STRING_FUNCTION = input -> {
        String output = input + " !!!";
        log.info("function applied (input / output) : " + input + "/" + output);
        return output;
    };

    Consumer<String> STRING_CONSUMER = input -> log.info("consumer accepted (input) : " + input);
    Consumer<Void> VOID_CONSUMER = aVoid -> log.info("consumer accepted (input) : " + aVoid);

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
                .runAsync(RUNNABLE) // -> CompletableFuture<Void>
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
                .supplyAsync(STRING_SUPPLIER1) // -> CompletableFuture<String>
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
                .runAsync(RUNNABLE)         // -> CompletableFuture<Void>
                .thenAccept(VOID_CONSUMER); // -> CompletableFuture<Void>

        CompletableFuture
                .supplyAsync(STRING_SUPPLIER1)  // -> CompletableFuture<String>
                .thenAccept(STRING_CONSUMER);   // -> CompletableFuture<Void>

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
    public void thenAccept_중첩() throws InterruptedException, ExecutionException {
        CompletableFuture
                .supplyAsync(STRING_SUPPLIER1)  // -> CompletableFuture<String>
                .thenAccept(STRING_CONSUMER)    // -> CompletableFuture<Void>
                .thenAccept(VOID_CONSUMER)      // -> CompletableFuture<Void>
                .get();
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
    public void thenApply_중첩() throws InterruptedException, ExecutionException {
        CompletableFuture
                .supplyAsync(STRING_SUPPLIER1)  // -> CompletableFuture<String>
                .thenApply(STRING_FUNCTION)     // -> CompletableFuture<String>
                .thenAccept(STRING_CONSUMER)    // -> CompletableFuture<Void>
                .get();
        /*
        supplyAsync(supplier<S>)의 결과인 <S>를 계속 전달하려면,
        thenApply(function<I, O>)를 호출해서 결과를 다음 단계의 .thenXxx()로 넘겨야 한다.

        [Result]
        00:19:06.188 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - supplier supplied (output) : test
        00:19:06.191 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - function applied (input / output) : test/test
        00:19:06.191 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - consumer accepted (input) : test
         */
    }

    @Test
    public void thenCompose() throws InterruptedException, ExecutionException {
        CompletableFuture
                .supplyAsync(STRING_SUPPLIER1)  // -> CompletableFuture<String>
                .thenCompose(input ->
                        CompletableFuture.completedFuture(input + " !!!") // -> CompletableFuture<String>
                )
                .thenAccept(STRING_CONSUMER)    // -> CompletableFuture<Void>
                .get();
        /*
        CompletableFuture와 또 다른 CompletableFuture를 연속해서(이어서) 실행해야 하는 경우에는
        thenAccept(consumer) or thenSupplier(supplier) or thenApply(function) 대신
        thenCompose(Function<I, ? extends CompletableFuture<O>>)를 사용하면 된다.

        [Result]
        12:56:18.744 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - supplier supplied (output) : test
        12:56:18.747 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - consumer accepted (input) : test !!!
         */
    }

    @Test
    public void thenCombine() throws ExecutionException, InterruptedException {
        BiFunction<String, String, String> combineBiFunction = (cfResult, anotherCfResult) -> { // -> String
            String output = cfResult + anotherCfResult;
            log.info("BiFunction(" + cfResult + ", " + anotherCfResult + ") -> " + output);
            return output;
        };

        String result = CompletableFuture
                .supplyAsync(STRING_SUPPLIER1)  // -> CompletableFuture<String>
                .thenCombine(
                        CompletableFuture.supplyAsync(STRING_SUPPLIER2), // -> CompletableFuture<String>
                        combineBiFunction
                )   // -> CompletableFuture<String>
                .get();
        log.info("result: " + result);
        /*
        CompletableFuture와 또 다른 CompletableFuture를 동시에 실행한 후 그 결과를 조합해야 하는 경우에는
        cf.thenCombine(anotherCf, BiFunction<cfResult, anotherCfResult, output>) 을 사용한다.
        cf와 anotherCf는 서로 다른 스레드에서 실행된 후 결과가 BiFunction으로 전달된다.
        BiFunction은 cf가 실행된 스레드 혹은 anotherCf가 실행된 스레드에서 실행된다.

        [Result]
        13:57:07.568 [ForkJoinPool.commonPool-worker-2] INFO async.CompletableFutureTest - another supplier supplied (output) :  !!!
        13:57:07.568 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - supplier supplied (output) : test
        13:57:07.571 [ForkJoinPool.commonPool-worker-2] INFO async.CompletableFutureTest - BiFunction(test,  !!!) -> test !!! <- worker2
        13:57:07.571 [main] INFO async.CompletableFutureTest - result: test !!!

        13:58:03.947 [ForkJoinPool.commonPool-worker-2] INFO async.CompletableFutureTest - another supplier supplied (output) :  !!!
        13:58:03.947 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - supplier supplied (output) : test
        13:58:03.949 [ForkJoinPool.commonPool-worker-1] INFO async.CompletableFutureTest - BiFunction(test,  !!!) -> test !!!  <- worker1
        13:58:03.949 [main] INFO async.CompletableFutureTest - result: test !!!
         */
    }
}
