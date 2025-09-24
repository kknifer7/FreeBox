package io.knifer.freebox.service;

import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ToastHelper;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * websocket通信服务
 * 发送请求-等待并获取结果
 *
 * @author Knifer
 */
@Slf4j
public class FutureWaitingService<T> extends Service<T> {

    /**
     * 要等待的Future对象
     */
    private final Future<T> future;

    /**
     * 是否设置超时
     */
    private final boolean needTimeout;

    /**
     * 排除显示错误提示的异常类型
     */
    private final Set<Class<? extends Throwable>> ignoringToastThrowableClasses;

    public FutureWaitingService(Future<T> future) {
        this(future, Set.of());
    }

    public FutureWaitingService(Future<T> future, Set<Class<? extends Throwable>> ignoringToastThrowableClasses) {
        this.future = future;
        this.ignoringToastThrowableClasses = ignoringToastThrowableClasses;
        this.needTimeout = true;
    }

    public FutureWaitingService(Future<T> future, boolean needTimeout) {
        this.future = future;
        this.needTimeout = needTimeout;
        this.ignoringToastThrowableClasses = Set.of();
    }

    @Override
    protected Task<T> createTask() {

        return new Task<>() {
            @Override
            protected T call() {
                try {
                    return needTimeout ?
                            future.get() :
                            future.get(BaseValues.KEB_SOCKET_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                } catch (TimeoutException | ExecutionException e) {
                    if (ignoringToastThrowableClasses.contains(e.getClass())) {
                        log.warn("Ignored future waiting timeout exception", e);
                    } else {
                        Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.COMMON_MESSAGE_TIMOUT_FAILED));
                    }
                } catch (Exception e) {
                    // InterruptedException | JsonSyntaxException
                    if (ignoringToastThrowableClasses.contains(e.getClass())) {
                        log.warn("Ignored future waiting exception", e);
                    } else {
                        Platform.runLater(() -> ToastHelper.showException(e));
                    }
                }

                return null;
            }
        };
    }
}
