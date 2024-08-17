package io.knifer.freebox.context;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.net.http.FreeBoxHttpServerHolder;
import io.knifer.freebox.net.websocket.FreeBoxWebSocketServerHolder;
import io.knifer.freebox.service.LoadConfigService;
import io.knifer.freebox.service.LoadNetworkInterfaceDataService;
import io.knifer.freebox.util.NetworkUtil;
import javafx.application.Application;
import javafx.concurrent.Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.net.NetworkInterface;
import java.util.Collection;

/**
 * 全局上下文
 * 不应在FX主线程之外访问它
 *
 * @author Knifer
 */
@Slf4j
public enum Context {

    INSTANCE;

    private Application app;

    private FreeBoxHttpServerHolder httpServer;

    private FreeBoxWebSocketServerHolder wsServer;

    private volatile boolean initFlag = false;

    public Application getApp() {
        if (app == null) {
            throw new IllegalStateException("application has not started yet");
        }

        return app;
    }

    public void init(Application app, FreeBoxHttpServerHolder httpServer, FreeBoxWebSocketServerHolder wsServer) {
        Service<Void> loadConfigService = new LoadConfigService();

        this.app = app;
        this.httpServer = httpServer;
        this.wsServer = wsServer;
        loadConfigService.setOnSucceeded(evt -> {
            autoStartServiceIfNeeded();
            log.info("application initialized");
            initFlag = true;
        });
        loadConfigService.start();
    }

    private void autoStartServiceIfNeeded() {
        String ip = ConfigHelper.getServiceIPv4();
        Service<Collection<Pair<NetworkInterface, String>>> loadNetworkInterfaceService;
        boolean httpAutoStartFlag = BooleanUtils.toBoolean(ConfigHelper.getAutoStartHttp());
        boolean wsAutoStartFlag = BooleanUtils.toBoolean(ConfigHelper.getAutoStartWs());
        boolean needAutoStart = httpAutoStartFlag || wsAutoStartFlag;

        if (!needAutoStart) {
            return;
        }
        if (StringUtils.isBlank(ip)) {
            log.info("no available ip, skip auto start service");
            return;
        }
        loadNetworkInterfaceService = new LoadNetworkInterfaceDataService();
        loadNetworkInterfaceService.setOnSucceeded(evt -> {
            boolean ipChangedFlag = loadNetworkInterfaceService.getValue()
                    .stream()
                    .noneMatch(pair -> ip.equals(pair.getValue()));
            int port;

            if (ipChangedFlag) {
                 ToastHelper.showInfoI18n(I18nKeys.INIT_IP_CHANGED_MESSAGE);

                 return;
            }
            if (httpAutoStartFlag) {
                port = ConfigHelper.getHttpPort();

                if (NetworkUtil.isPortUsing(port)) {
                    ToastHelper.showError(String.format(
                            I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                            port
                    ));
                } else {
                    httpServer.start(ip, port);
                    log.info("http service started");
                }
            }
            if(wsAutoStartFlag) {
                port = ConfigHelper.getWsPort();

                if (NetworkUtil.isPortUsing(port)) {
                    ToastHelper.showError(String.format(
                            I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                            port
                    ));
                } else {
                    wsServer.start(ip, port);
                    log.info("websocket service started");
                }
            }

        });
        loadNetworkInterfaceService.start();
    }

    public boolean isInitialized() {
        return initFlag;
    }

    public FreeBoxHttpServerHolder getHttpServer() {
        return httpServer;
    }

    public FreeBoxWebSocketServerHolder getWsServer() {
        return wsServer;
    }

    public void destroy() {
        if (httpServer.isRunning()) {
            log.info("stopping http service......");
            httpServer.stopBlocking();
        }
        if (wsServer.isRunning()) {
            log.info("stopping websocket service......");
            wsServer.stopBlocking();
        }
    }
}
