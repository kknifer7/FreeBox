package io.knifer.freebox.net.http.server;

import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.SimpleServer;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.net.http.handler.*;
import io.knifer.freebox.service.ShutdownHttpServerService;
import javafx.concurrent.Service;

import java.util.List;

/**
 * HTTP服务
 *
 * @author Knifer
 */
public class FreeBoxHttpServerHolder {

    private volatile SimpleServer server;

    private static final List<HttpHandler> HANDLERS = List.of(
            new TVBoxPairingInfoHandler(),
            new ProxyCkHandler(),
            new ProxyHandler(),
            new MsgHandler()
    );

    public synchronized void start(String hostname, int port) {
        try {
            if (BaseValues.ANY_LOCAL_IP.equals(hostname)) {
                server = HttpUtil.createServer(port);
            } else {
                server = new SimpleServer(hostname, port);
            }
            server.addHandler("/", exchange -> {
                HANDLERS.forEach(handler -> {
                    if (handler.support(exchange)) {
                        handler.handle(exchange);
                    }
                });
            });
            server.start();
        } catch (Exception e) {
            ToastHelper.showException(e);

            return;
        }
        Context.INSTANCE.postEvent(AppEvents.HTTP_SERVER_STARTED);
    }

    public synchronized void stop(Runnable callback) {
        Service<Void> service;

        if (isRunning()) {
            service = new ShutdownHttpServerService(server.getRawServer());
            service.setOnSucceeded(evt -> {
                server = null;
                callback.run();
            });
            service.start();
        }
    }

    public synchronized void stopBlocking() {
        if (isRunning()) {
            server.getRawServer().stop(2);
        }
    }

    public boolean isRunning() {
        return server != null;
    }
}
