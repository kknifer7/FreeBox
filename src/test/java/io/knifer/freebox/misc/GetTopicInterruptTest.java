package io.knifer.freebox.misc;

import org.apache.commons.lang3.ThreadUtils;
import org.thavam.util.concurrent.blockingMap.BlockingHashMap;
import org.thavam.util.concurrent.blockingMap.BlockingMap;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Knifer
 */
public class GetTopicInterruptTest {

    public static void main(String[] args) {
        BlockingMap<String, String> map = new BlockingHashMap<>();
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                return map.take("test", 10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.out.println("interrupt success!!!");
                throw new RuntimeException(e);
            }
        }).thenApply(ignored -> {
            System.err.println("failed!!!!");

            return null;
        }).exceptionally(ex -> {
            ex.printStackTrace();

            return null;
        });

        ThreadUtils.sleepQuietly(Duration.ofSeconds(1));
        future.cancel(true);
        ThreadUtils.sleepQuietly(Duration.ofSeconds(100));
    }
}
