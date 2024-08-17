package io.knifer.freebox.service;

import io.knifer.freebox.net.websocket.FreeBoxWebSocketServer;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;

/**
 * 关闭WS服务
 *
 * @author Knifer
 */
@AllArgsConstructor
public class ShutdownWebSocketServerService extends Service<Void> {

    private final FreeBoxWebSocketServer server;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    server.stop(2);
                } catch (InterruptedException ignored) {}

                return null;
            }
        };
    }
}
