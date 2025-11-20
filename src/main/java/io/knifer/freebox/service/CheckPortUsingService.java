package io.knifer.freebox.service;

import io.knifer.freebox.util.NetworkUtil;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 检测端口是否被占用服务
 *
 * @author Knifer
 */
@Getter
@AllArgsConstructor
public class CheckPortUsingService extends Service<Boolean> {

    private final String hostname;

    private final Integer port;

    public CheckPortUsingService(Integer port) {
        this("localhost", port);
    }

    @Override
    protected Task<Boolean> createTask() {
        return new Task<>() {
            @Override
            protected Boolean call() {
                return NetworkUtil.isPortUsing(hostname, port, 500);
            }
        };
    }
}
