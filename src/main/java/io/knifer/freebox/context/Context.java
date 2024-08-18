package io.knifer.freebox.context;

import com.google.common.eventbus.EventBus;
import io.knifer.freebox.component.event.EventListener;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.net.ServiceManager;
import io.knifer.freebox.net.http.server.FreeBoxHttpServerHolder;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.server.KebSocketServerHolder;
import io.knifer.freebox.service.LoadConfigService;
import javafx.application.Application;
import javafx.concurrent.Service;
import lombok.extern.slf4j.Slf4j;

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

    private final ServiceManager serviceManager = new ServiceManager();

    private volatile boolean initFlag = false;

    private final EventBus eventBus = new EventBus((exception, context) -> {
        if (exception instanceof ClassCastException) {
            // 忽略ClassCastException，原因见EventListener
            return;
        }
        ToastHelper.showException(exception);
    });

    public Application getApp() {
        if (app == null) {
            throw new IllegalStateException("application has not started yet");
        }

        return app;
    }

    public void init(Application app, Runnable callback) {
        Service<Void> loadConfigService = new LoadConfigService();

        this.app = app;
        loadConfigService.setOnSucceeded(evt -> {
            serviceManager.init(callback);
            log.info("application initialized");
            initFlag = true;
        });
        loadConfigService.start();
    }

    public boolean isInitialized() {
        return initFlag;
    }

    public FreeBoxHttpServerHolder getHttpServer() {
        return serviceManager.getHttpServer();
    }

    public KebSocketServerHolder getWsServer() {
        return serviceManager.getWsServer();
    }

    public ClientManager getClientManager() {
        return getWsServer().getClientManager();
    }

    public void destroy() {
        serviceManager.destroy();
    }

    public void postEvent(Object object) {
        eventBus.post(object);
    }

    public <T extends AppEvents.Event> void registerEventListener(T event, EventListener<T> listener) {
        eventBus.register(listener);
    }

    public <T extends AppEvents.Event> void registerEventListener(Class<T> eventClazz, EventListener<T> listener) {
        eventBus.register(listener);
    }
}
