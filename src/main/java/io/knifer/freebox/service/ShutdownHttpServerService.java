package io.knifer.freebox.service;

import com.sun.net.httpserver.HttpServer;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;

/**
 * 关闭HTTP服务器服务
 *
 * @author Knifer
 */
@AllArgsConstructor
public class ShutdownHttpServerService extends Service<Void> {

    private final HttpServer server;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                server.stop(0);

                return null;
            }
        };
    }
}
