package io.knifer.freebox.context;

import com.google.common.eventbus.EventBus;
import io.knifer.freebox.component.event.EventListener;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.net.ServiceManager;
import io.knifer.freebox.net.http.server.FreeBoxHttpServerHolder;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.core.KebSocketTopicKeeper;
import io.knifer.freebox.net.websocket.server.KebSocketServerHolder;
import io.knifer.freebox.service.LoadConfigService;
import io.knifer.freebox.util.AsyncUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Stack;

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

    private Stage primaryStage;

    private final ServiceManager serviceManager = new ServiceManager();

    private volatile boolean initFlag = false;

    private final EventBus eventBus = new EventBus((exception, context) -> {
        if (exception instanceof ClassCastException) {
            // 忽略ClassCastException，原因见EventListener
            return;
        }
        ToastHelper.showException(exception);
    });

    private final Stack<Stage> stageStack = new Stack<>();

    public Application getApp() {
        if (!initFlag) {
            throw new IllegalStateException("application has not started yet");
        }

        return app;
    }

    public Stage getPrimaryStage() {
        if (!initFlag) {
            throw new IllegalStateException("application has not started yet");
        }

        return primaryStage;
    }

    public void init(Application app, Stage primaryStage, Runnable callback) {
        Service<Void> loadConfigService = new LoadConfigService();

        this.app = app;
        this.primaryStage = primaryStage;
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
        AsyncUtil.destroy();
        Context.INSTANCE.getPrimaryStage().close();
        KebSocketTopicKeeper.getInstance().destroy();
        Platform.exit();
        System.exit(0);
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

    public void pushStage(Stage stage) {
        if (stageStack.contains(stage)) {
            return;
        }
        stageStack.push(stage);
    }

    public void popAndShowLastStage() {
        Stage stage = popStage();

        if (stage != null) {
            stage.show();
        }
    }

    @Nullable
    public Stage popStage() {
        if (stageStack.isEmpty()) {
            return null;
        }

        return stageStack.pop();
    }
}
