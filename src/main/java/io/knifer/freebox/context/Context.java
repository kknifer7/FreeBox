package io.knifer.freebox.context;

import com.google.common.eventbus.EventBus;
import io.knifer.freebox.component.event.EventListener;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.net.http.FreeBoxHttpServerHolder;
import io.knifer.freebox.net.websocket.FreeBoxWebSocketServerHolder;
import io.knifer.freebox.service.CheckPortUsingService;
import io.knifer.freebox.service.LoadConfigService;
import io.knifer.freebox.service.LoadNetworkInterfaceDataService;
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

    private static final EventBus EVENT_BUS = new EventBus();

    public Application getApp() {
        if (app == null) {
            throw new IllegalStateException("application has not started yet");
        }

        return app;
    }

    public void init(
            Application app, FreeBoxHttpServerHolder httpServer, FreeBoxWebSocketServerHolder wsServer, Runnable callback
    ) {
        Service<Void> loadConfigService = new LoadConfigService();

        this.app = app;
        this.httpServer = httpServer;
        this.wsServer = wsServer;
        loadConfigService.setOnSucceeded(evt -> {
            autoStartServiceIfNeeded(callback);
            log.info("application initialized");
            initFlag = true;
        });
        loadConfigService.start();
    }

    private void autoStartServiceIfNeeded(Runnable callback) {
        String ip = ConfigHelper.getServiceIPv4();
        Service<Collection<Pair<NetworkInterface, String>>> loadNetworkInterfaceService;
        boolean httpAutoStartFlag = BooleanUtils.toBoolean(ConfigHelper.getAutoStartHttp());
        boolean wsAutoStartFlag = BooleanUtils.toBoolean(ConfigHelper.getAutoStartWs());
        boolean needAutoStart = httpAutoStartFlag || wsAutoStartFlag;

        if (!needAutoStart) {
            callback.run();

            return;
        }
        if (StringUtils.isBlank(ip)) {
            log.info("no available ip, skip auto start service");
            callback.run();

            return;
        }
        loadNetworkInterfaceService = new LoadNetworkInterfaceDataService();
        loadNetworkInterfaceService.setOnSucceeded(evt -> {
            boolean ipChangedFlag = loadNetworkInterfaceService.getValue()
                    .stream()
                    .noneMatch(pair -> ip.equals(pair.getValue()));
            int port;
            CheckPortUsingService httpCheckPortUsingService;
            CheckPortUsingService wsCheckPortUsingService;

            if (ipChangedFlag) {
                 ToastHelper.showInfoI18n(I18nKeys.INIT_IP_CHANGED_MESSAGE);
                 callback.run();

                 return;
            }
            if (httpAutoStartFlag) {
                port = ConfigHelper.getHttpPort();
                httpCheckPortUsingService = new CheckPortUsingService(ip, port);
                httpCheckPortUsingService.setOnSucceeded(ignore -> {
                    Boolean isPortUsing = httpCheckPortUsingService.getValue();
                    Integer httpPort = httpCheckPortUsingService.getPort();

                    if (isPortUsing) {
                        ToastHelper.showError(String.format(
                                I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                                httpPort
                        ));
                    } else {
                        httpServer.start(ip, httpPort);
                        log.info("http service started");
                    }
                    if (!wsAutoStartFlag) {
                        // 启动http服务后，如果不需要再启动ws服务，就直接触发init的完成回调
                        callback.run();
                    }
                });
                httpCheckPortUsingService.start();
            }
            if(wsAutoStartFlag) {
                port = ConfigHelper.getWsPort();
                wsCheckPortUsingService = new CheckPortUsingService(ip, port);
                wsCheckPortUsingService.setOnSucceeded(ignore -> {
                    Boolean isPortUsing = wsCheckPortUsingService.getValue();
                    Integer wsPort = wsCheckPortUsingService.getPort();

                    if (isPortUsing) {
                        ToastHelper.showError(String.format(
                                I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                                wsPort
                        ));
                    } else {
                        wsServer.start(ip, wsPort);
                        log.info("websocket service started");
                    }
                    callback.run();
                });
                wsCheckPortUsingService.start();
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

    public void postEvent(Object object) {
        EVENT_BUS.post(object);
    }

    public <T extends AppEvents.Event> void registerEventListener(T event, EventListener<T> listener) {
        EVENT_BUS.register(listener);
    }
}
