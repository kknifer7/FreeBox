package io.knifer.freebox.context;

import com.google.common.eventbus.EventBus;
import io.knifer.freebox.component.event.EventListener;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.ClientType;
import io.knifer.freebox.handler.impl.SingleInstanceApplicationHandler;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.StorageHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.net.ServiceManager;
import io.knifer.freebox.net.http.server.FreeBoxHttpServerHolder;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.core.KebSocketRunner;
import io.knifer.freebox.net.websocket.core.KebSocketTopicKeeper;
import io.knifer.freebox.net.websocket.server.KebSocketServerHolder;
import io.knifer.freebox.service.LoadConfigService;
import io.knifer.freebox.service.SingleInstanceApplicationService;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.spider.template.impl.FreeBoxSpiderTemplate;
import io.knifer.freebox.spider.template.impl.KebSocketSpiderTemplate;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CollectionUtil;
import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Setter
    private Stage currentStage;

    private Map<ClientType, SpiderTemplate> spiderTemplateMap;

    private volatile boolean initFlag = false;

    @Getter
    private final ClientManager clientManager = new ClientManager();

    private final ServiceManager serviceManager = new ServiceManager(clientManager);

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

    public Stage getCurrentStage() {
        return currentStage == null ? primaryStage : currentStage;
    }

    public void init(Application app, Stage primaryStage, Runnable callback) {
        Service<Void> loadConfigService = new LoadConfigService();

        this.app = app;
        this.primaryStage = primaryStage;
        registerEventListener(AppEvents.WsServerStartedEvent.class, evt -> {
            // 初始化websocket模板
            spiderTemplateMap = Map.of(
                    ClientType.TVBOX_K, new KebSocketSpiderTemplate(KebSocketRunner.getInstance(), clientManager),
                    ClientType.CATVOD_SPIDER, new FreeBoxSpiderTemplate(clientManager)
            );
        });
        // 配置读取
        loadConfigService.setOnSucceeded(evt -> {
            Service<Void> singleInstanceAppService = new SingleInstanceApplicationService();

            // 确保应用单例
            singleInstanceAppService.setOnSucceeded(ignored -> {
                Integer appVersionCode;
                String latestAppVersionCodeStr;
                int latestAppVersionCode;
                String latestAppVersion;

                // 处理版本升级相关
                appVersionCode = ConfigHelper.getAppVersionCode();
                latestAppVersionCodeStr = BaseResources.X_PROPERTIES.getProperty(BaseValues.X_APP_VERSION_CODE);
                latestAppVersionCode = NumberUtils.isCreatable(latestAppVersionCodeStr) ?
                        Integer.parseInt(latestAppVersionCodeStr) : 0;
                if (appVersionCode < latestAppVersionCode) {
                    // config版本与x.properties中的版本不一致，说明用户刚刚安装了新版本，可对比新旧版本号，做一些更新后置操作
                    // ...
                    // 后置操作完成，保存最新版本号到config
                    latestAppVersion = BaseResources.X_PROPERTIES.getProperty(BaseValues.X_APP_VERSION);
                    ConfigHelper.setAppVersion(latestAppVersion);
                    ConfigHelper.setAppVersionCode(latestAppVersionCode);
                    ConfigHelper.saveAnyWay(() -> {
                        log.info("application upgraded to {}", latestAppVersion);
                        doInit(callback);
                    });
                } else {
                    doInit(callback);
                }
            });
            singleInstanceAppService.start();
        });
        loadConfigService.start();
    }

    private void doInit(Runnable callback) {
        // 清理临时目录
        StorageHelper.clearTemp();
        // 初始化服务管理器
        serviceManager.init(callback);
        log.info("application initialized");
        initFlag = true;
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

    public SpiderTemplate getSpiderTemplate() {
        ClientInfo clientInfo = getClientManager().getCurrentClientImmediately();

        if (clientInfo == null) {
            throw new IllegalStateException("no client connected");
        }

        return spiderTemplateMap.get(clientInfo.getClientType());
    }

    public void destroy() {
        serviceManager.destroy();
        AsyncUtil.destroy();
        KebSocketTopicKeeper.getInstance().destroy();
        if (initFlag) {
            Context.INSTANCE.getPrimaryStage().close();
            if (CollectionUtil.isNotEmpty(spiderTemplateMap)) {
                spiderTemplateMap.values().forEach(SpiderTemplate::destroy);
            }
        }
        SingleInstanceApplicationHandler.getInstance().releaseLock();
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

    public List<Stage> popAllStage() {
        List<Stage> stageList;

        if (stageStack.isEmpty()) {

            return List.of();
        }
        stageList = new ArrayList<>(stageStack.size());
        while (!stageStack.isEmpty()) {
            stageList.add(stageStack.pop());
        }

        return stageList;
    }
}
