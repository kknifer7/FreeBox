package io.knifer.freebox.net.websocket.server;

import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.service.ShutdownWebSocketServerService;
import javafx.application.Platform;
import javafx.concurrent.Service;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WS服务器
 *
 * @author Knifer
 */
public class KebSocketServerHolder {

    private final AtomicReference<KebSocketServer> server = new AtomicReference<>(null);
    private final ClientManager clientManager;

    public KebSocketServerHolder(ClientManager clientManager) {
        this.clientManager = clientManager;
        Platform.runLater(() ->
            Context.INSTANCE.registerEventListener(
                    AppEvents.WsServerStartedEvent.class,
                    evt -> this.server.set(evt.server())
            )
        );
    }

    public synchronized void start(String hostname, int port) {
        KebSocketServer wsServer = new KebSocketServer(
                BaseValues.ANY_LOCAL_IP.equals(hostname) ?
                        new InetSocketAddress(port) : new InetSocketAddress(hostname, port),
                clientManager
        );

        wsServer.start();
    }

    public synchronized void stop(Runnable callback) {
        Service<Void> service;

        if (isRunning()) {
            service = new ShutdownWebSocketServerService(server.get());
            service.setOnSucceeded(evt -> {
                this.server.set(null);
                callback.run();
            });
            service.start();
        }
    }

    public synchronized void stopBlocking() {
        if (isRunning()) {
            server.updateAndGet(server -> {
                try {
                    if (server != null) {
                        server.stop(2);
                    }
                } catch (InterruptedException ignored) {}

                return null;
            });
        }
    }

    public boolean isRunning() {
        return server.get() != null;
    }
}
