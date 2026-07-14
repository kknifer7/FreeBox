package io.knifer.freebox.component.juc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * 可中断的 CompletableFuture 包装
 * <p>
 * 调用 {@link #cancel(boolean)} 时会同时取消底层任务，使阻塞操作（如 {@code BlockingMap#take}）能被真正 interrupt。
 *
 * @author Knifer
 */
public class InterruptibleCompletableFuture<T> extends CompletableFuture<T> {

    private final Future<?> task;

    public InterruptibleCompletableFuture(CompletableFuture<T> delegate, Future<?> task) {
        this.task = task;
        delegate.whenComplete((result, exception) -> {
            if (exception != null) {
                completeExceptionally(exception);

                return;
            }
            complete(result);
        });
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        task.cancel(mayInterruptIfRunning);

        return super.cancel(mayInterruptIfRunning);
    }
}
