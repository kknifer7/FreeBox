package io.knifer.freebox.net.websocket.server;

import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.service.ShutdownWebSocketServerService;
import javafx.concurrent.Service;

import java.net.InetSocketAddress;

/**
 * WS服务器
 *
 * @author Knifer
 */
public class KebSocketServerHolder {

    private volatile KebSocketServer server;

    public synchronized void start(String hostname, int port) {
        server = new KebSocketServer(new InetSocketAddress(hostname, port));
        server.start();
        Context.INSTANCE.postEvent(AppEvents.WS_SERVER_STARTED);
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

    public ClientManager getClientManager() {
        return server.getClientManager();
    }
}
