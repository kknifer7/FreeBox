package io.knifer.freebox.util;

import io.knifer.freebox.exception.GlobalExceptionHandler;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 异步操作工具类
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class AsyncUtil {

    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            5,
            5,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r);

                t.setName("AsyncUtil-Thread");
                t.setUncaughtExceptionHandler(GlobalExceptionHandler.getInstance());

                return t;
            }
    );

    public <R> void execute(Callable<R> callable, Consumer<R> consumer) {
        EXECUTOR.execute(() -> {
            try {
                consumer.accept(callable.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void execute(Runnable runnable) {
        EXECUTOR.execute(runnable);
    }

    public void cancelAllTask() {
        EXECUTOR.getQueue().clear();
    }

    public void destroy() {
        log.info("shutdown AsyncUtil ThreadPoolExecutor......");
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("AsyncUtil ThreadPoolExecutor termination timeout, force shutdown......");
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException ignored) {}
    }
}
