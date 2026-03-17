package io.knifer.freebox.net.http.server;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.SimpleServer;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.net.http.handler.*;
import io.knifer.freebox.service.ShutdownHttpServerService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
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
@Singleton
public class FreeBoxHttpServerHolder {

    private volatile SimpleServer server;

    // 通过Provider注入，避免循环依赖
    private final Provider<Context> contextProvider;
    private final List<HttpHandler> handlers;

    @Inject
    public FreeBoxHttpServerHolder(
            Provider<Context> contextProvider,
            TVBoxPairingInfoHandler tvBoxPairingInfoHandler,
            MsgHandler msgHandler,
            ProxyCkHandler proxyCkHandler,
            ProxyHandler proxyHandler,
            ProxyCacheHandler proxyCacheHandler,
            ProxyTsHandler proxyTsHandler,
            ProxyLiveM3uHandler proxyLiveM3uHandler
    ) {
        this.contextProvider = contextProvider;
        this.handlers = List.of(
                tvBoxPairingInfoHandler,
                msgHandler,
                proxyCkHandler,
                proxyHandler,
                proxyCacheHandler,
                proxyTsHandler,
                proxyLiveM3uHandler
        );
    }

    public synchronized boolean start(String hostname, int port) {
        try {
            if (BaseValues.ANY_LOCAL_IP.equals(hostname)) {
                server = HttpUtil.createServer(port);
            } else {
                server = new SimpleServer(hostname, port);
            }
            server.addHandler("/", exchange ->
                handlers.forEach(handler -> {
                    if (handler.support(exchange)) {
                        handler.handle(exchange);
                    }
                })
            );
            server.start();
            log.info("http service start successfully");
            contextProvider.get().postEvent(AppEvents.HTTP_SERVER_STARTED);

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
