package io.knifer.freebox.net.websocket;

import io.knifer.freebox.service.ShutdownWebSocketServerService;
import javafx.concurrent.Service;

import java.net.InetSocketAddress;

/**
 * WS服务器
 *
 * @author Knifer
 */
public class FreeBoxWebSocketServerHolder {

    private volatile FreeBoxWebSocketServer server;

    public synchronized void start(String hostname, int port) {
        server = new FreeBoxWebSocketServer(new InetSocketAddress(hostname, port));
        server.start();
    }

    public synchronized void stop(Runnable callback) {
        Service<Void> service;

        if (isRunning()) {
            service = new ShutdownWebSocketServerService(server);
            service.setOnSucceeded(evt -> {
                server = null;
                callback.run();
            });
            service.start();
        }
    }

    public synchronized void stopBlocking() {
        if (isRunning()) {
            try {
                server.stop(2);
            } catch (InterruptedException ignored) {}
            server = null;
        }
    }

    public boolean isRunning() {
        return server != null;
    }
}