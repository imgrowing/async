package async;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.*;

@Slf4j
public class CompletableFutureWithTimeoutTest {

    public static ScheduledExecutorService getDelayer() {
        return Executors.newScheduledThreadPool(10);
    }

    private void delayMillis(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public <T> CompletableFuture<T> timeoutAfter(long timeout, TimeUnit unit) {
        CompletableFuture<T> result = new CompletableFuture<T>();
        getDelayer().schedule(
                () -> {
                    log.info("completeExceptionally : TimeoutException");
                    result.completeExceptionally(new TimeoutException());
                },
                timeout, unit
        );
        return result;
    }

    public String sendMessage(String message, int delay) {
        delayMillis(delay);
        log.info("sendMessage - " + message);
        return message;
    }

    @Test
    public void timeoutWithAsyncTask() {
        String message = "test message";

        // TODO 처리해야 할 작업이 동시에 많이 발생한다면, 해당 작업을 위한 전용 ExectorService를 supplyAsync에 파라미터로 전달하도록 하자
        CompletableFuture
                .supplyAsync(() -> this.sendMessage(message, 300))           // delay를 변경해서 실행 (선/후)
                .acceptEither(
                        timeoutAfter(100, TimeUnit.MILLISECONDS),          // delay를 변경해서 실행 (선/후)
                        // supplyAsync()가 정상적으로 완료된 경우에만 실행됨
                        result -> log.info("acceptEither() - result: " + result)
                )
                .exceptionally(exception -> {
                    // supplyAsync()가 비정상적으로 완료되었거나, acceptEither() 내의 another CF가 비정상적으로 완료되었을 때 실행됨
                    log.info(">>> Error Occurred : " + exception.getMessage());
                    return null;
                });

        delayMillis(1000);

        /*
        [Result - 정상 처리가 timeout 안에 끝난 경우 (Timeout이 나중에 발생)] - message delay: 300, timeout: 500
        - acceptEither 내부의 consumer function 실행 : Y
        - exceptionally(Throwable) 실행 : N
        13:59:59.314 [ForkJoinPool.commonPool-worker-1] INFO - sendMessage - test message
        13:59:59.314 [ForkJoinPool.commonPool-worker-1] INFO - acceptEither() - result: test message  <<<<<<<<<
        13:59:59.513                  [pool-1-thread-1] INFO - completeExceptionally : TimeoutException

        [Result - 정상 처리가 timeout 안에 끝나지 못한 경우 (Timeout이 먼저 발생)] - message delay: 300, timeout: 100
        - acceptEither 내부의 consumer function 실행 : N
        - exceptionally(Throwable) 실행 : Y
        14:01:46.683                  [pool-1-thread-1] INFO - completeExceptionally : TimeoutException
        14:01:46.683                  [pool-1-thread-1] INFO - >>> Error Occurred : java.util.concurrent.TimeoutException
        14:01:46.880 [ForkJoinPool.commonPool-worker-1] INFO - sendMessage - test message
         */
    }
}
