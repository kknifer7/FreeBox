package io.knifer.freebox.net.http.server;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.SimpleServer;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.net.http.handler.*;
import io.knifer.freebox.service.ShutdownHttpServerService;
import javafx.application.Platform;
import javafx.concurrent.Service;
import lombok.extern.slf4j.Slf4j;

import java.net.BindException;
import java.util.List;

/**
 * HTTP服务
 *
 * @author Knifer
 */
@Slf4j
public class FreeBoxHttpServerHolder {

    private volatile SimpleServer server;

    private static final List<HttpHandler> HANDLERS = List.of(
            new TVBoxPairingInfoHandler(),
            new MsgHandler(),
            new ProxyCkHandler(),
            new ProxyHandler(),
            new ProxyCacheHandler(),
            new ProxyTsHandler()
    );

    public synchronized boolean start(String hostname, int port) {
        try {
            if (BaseValues.ANY_LOCAL_IP.equals(hostname)) {
                server = HttpUtil.createServer(port);
            } else {
                server = new SimpleServer(hostname, port);
            }
            server.addHandler("/", exchange ->
                HANDLERS.forEach(handler -> {
                    if (handler.support(exchange)) {
                        handler.handle(exchange);
                    }
                })
            );
            server.start();
            log.info("http service start successfully");
            Context.INSTANCE.postEvent(AppEvents.HTTP_SERVER_STARTED);

            return true;
        } catch (IORuntimeException e) {
            Platform.runLater(() -> {
                if (e.causeInstanceOf(BindException.class)) {
                    ToastHelper.showError(String.format(I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE), port));
                } else {
                    ToastHelper.showException(e);
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> ToastHelper.showException(e));
        }
        if (server != null) {
            stop(BaseValues.EMPTY_RUNNABLE);
        }

        return false;
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
