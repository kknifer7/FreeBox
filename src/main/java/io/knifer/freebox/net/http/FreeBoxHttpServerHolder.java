package io.knifer.freebox.net.http;

import com.sun.net.httpserver.HttpServer;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.service.ShutdownHttpServerService;
import javafx.concurrent.Service;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * HTTP服务
 *
 * @author Knifer
 */
public class FreeBoxHttpServerHolder {

    private volatile HttpServer server;

    private final static FreeBoxHttpHandler HANDLER = new FreeBoxHttpHandler();

    public synchronized void start(String hostname, int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(hostname, port), 0);
        } catch (IOException e) {
            ToastHelper.showException(e);
            return;
        }
        server.createContext("/", HANDLER);
        server.start();
    }

    public synchronized void stop(Runnable callback) {
        Service<Void> service;

        if (isRunning()) {
            service = new ShutdownHttpServerService(server);
            service.setOnSucceeded(evt -> {
                server = null;
                callback.run();
            });
            service.start();
        }
    }

    public synchronized void stopBlocking() {
        if (isRunning()) {
            server.stop(2);
        }
    }

    public boolean isRunning() {
        return server != null;
    }
}
