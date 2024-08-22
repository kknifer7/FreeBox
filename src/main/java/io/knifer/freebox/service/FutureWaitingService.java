package io.knifer.freebox.service;

import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.helper.ToastHelper;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;

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
@AllArgsConstructor
public class FutureWaitingService<T> extends Service<T> {

    private final Future<T> future;

    @Override
    protected Task<T> createTask() {
        return new Task<>() {
            @Override
            protected T call() {
                try {
                    return future.get(BaseValues.KEB_SOCKET_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    Platform.runLater(() -> ToastHelper.showException(e));

                    return null;
                }
            }
        };
    }
}
