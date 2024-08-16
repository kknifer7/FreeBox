package io.knifer.freebox.context;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.net.http.FreeBoxHttpServer;
import io.knifer.freebox.service.LoadConfigService;
import io.knifer.freebox.util.NetworkUtil;
import javafx.application.Application;
import javafx.concurrent.Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

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

    private FreeBoxHttpServer httpServer;

    private volatile boolean initFlag = false;

    public Application getApp() {
        if (app == null) {
            throw new IllegalStateException("application has not started yet");
        }

        return app;
    }

    public void init(Application app, FreeBoxHttpServer httpServer) {
        Service<Void> loadConfigService = new LoadConfigService();

        this.app = app;
        this.httpServer = httpServer;
        loadConfigService.setOnSucceeded(evt -> {
            autoStartServiceIfNeeded();
            log.info("application initialized");
            initFlag = true;
        });
        loadConfigService.start();
    }

    private void autoStartServiceIfNeeded() {
        String ip = ConfigHelper.getServiceIPv4();
        int httpPort;

        if (StringUtils.isBlank(ip)) {
            log.info("no available ip, skip auto start service");

            return;
        }
        if (BooleanUtils.toBoolean(ConfigHelper.getAutoStartHttp())) {
            httpPort = ConfigHelper.getHttpPort();
            if (NetworkUtil.isPortUsing(httpPort)) {
                ToastHelper.showError(String.format(
                        I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                        httpPort
                ));
            } else {
                httpServer.start(ip, httpPort);
                log.info("http service started");
            }
        }
    }

    public boolean isInitialized() {
        return initFlag;
    }

    public FreeBoxHttpServer getHttpServer() {
        return httpServer;
    }

    public void destroy() {
        if (httpServer.isRunning()) {
            log.info("stopping http service......");
            httpServer.stopBlocking();
        }
    }
}
