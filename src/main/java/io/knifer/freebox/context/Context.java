package io.knifer.freebox.context;

import com.google.common.eventbus.EventBus;
import io.knifer.freebox.component.event.EventListener;
import io.knifer.freebox.component.router.Router;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.ClientType;
import io.knifer.freebox.controller.BaseController;
import io.knifer.freebox.handler.impl.SingleInstanceApplicationHandler;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.StorageHelper;
import io.knifer.freebox.helper.SystemHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.ioc.IOC;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.net.ServiceManager;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.core.KebSocketTopicKeeper;
import io.knifer.freebox.service.SingleInstanceApplicationService;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.spider.template.impl.FreeBoxSpiderTemplate;
import io.knifer.freebox.spider.template.impl.KebSocketSpiderTemplate;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CollectionUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.tinylog.Level;

import java.util.Collection;
import java.util.Map;

/**
 * 全局上下文
 *
 * @author Knifer
 */
@Slf4j
@Singleton
public class Context {

    private Application app;

    private Map<ClientType, SpiderTemplate> spiderTemplateMap;

    private volatile boolean initFlag;

    private final ClientManager clientManager;

    private final ServiceManager serviceManager;

    private final KebSocketTopicKeeper kebSocketTopicKeeper;

    private final Router router;

    private final SingleInstanceApplicationHandler singleInstanceApplicationHandler;

    private final EventBus eventBus;

    @Inject
    public Context(
            ClientManager clientManager,
            ServiceManager serviceManager,
            KebSocketTopicKeeper kebSocketTopicKeeper,
            Router router,
            SingleInstanceApplicationHandler singleInstanceApplicationHandler
    ) {
        this.clientManager = clientManager;
        this.serviceManager = serviceManager;
        this.kebSocketTopicKeeper = kebSocketTopicKeeper;
        this.router = router;
        this.singleInstanceApplicationHandler = singleInstanceApplicationHandler;
        this.eventBus = new EventBus((exception, context) -> {
            if (exception instanceof ClassCastException) {
                // 忽略ClassCastException，原因见EventListener
                return;
            }
            ToastHelper.showException(exception);
        });
        this.initFlag = false;
    }

    public Application getApp() {
        if (!initFlag) {
            throw new IllegalStateException("application has not started yet");
        }

        return app;
    }

    public void init(Application app, Runnable callback) {
        Service<Void> singleInstanceAppService;
        Level logLevel;
        Stage primaryStage = router.getPrimary();

        this.app = app;
        primaryStage.setOnHidden(evt -> {
            Collection<Pair<Stage, ? extends BaseController>> secondaries = router.getSecondaries();

            if (!secondaries.isEmpty()) {
                log.debug("primary stage is closed, hide other windows to exit the application.");
                for (Pair<Stage, ? extends BaseController> pair : secondaries) {
                    pair.getLeft().hide();
                }
            }
        });
        logBaseInfo();
        // 配置日志等级
        logLevel = ConfigHelper.getLogLevel();
        if (logLevel != BaseValues.DEFAULT_LOG_LEVEL) {
            SystemHelper.reloadLogging();
        }
        registerEventListener(AppEvents.WsServerStartedEvent.class, evt -> {
            // 初始化websocket模板
            if (spiderTemplateMap == null) {
                spiderTemplateMap = Map.of(
                        ClientType.TVBOX_K, IOC.getBean(KebSocketSpiderTemplate.class),
                        ClientType.CATVOD_SPIDER, IOC.getBean(FreeBoxSpiderTemplate.class)
                );
            }
        });
        // 确保应用单例
        singleInstanceAppService = new SingleInstanceApplicationService();
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
    }

    private void logBaseInfo() {
        // 输出应用版本、系统信息
        log.info(
                """
                       \s
                           ___                     __                      \s
                         /'___\\                   /\\ \\                     \s
                        /\\ \\__/  _ __    __     __\\ \\ \\____    ___   __  _ \s
                        \\ \\ ,__\\/\\`'__\\/'__`\\ /'__`\\ \\ '__`\\  / __`\\/\\ \\/'\\\s
                         \\ \\ \\_/\\ \\ \\//\\  __//\\  __/\\ \\ \\L\\ \\/\\ \\L\\ \\/>  </\s
                          \\ \\_\\  \\ \\_\\\\ \\____\\ \\____\\\\ \\_,__/\\ \\____//\\_/\\_\\
                           \\/_/   \\/_/ \\/____/\\/____/ \\/___/  \\/___/ \\//\\/_/
                                                                           \s
                                                                           \s
                       \s
               {}\s
               ------------------------
               """, SystemHelper.getSystemSummary()
        );
    }

    private void doInit(Runnable callback) {
        // 清理临时目录
        StorageHelper.clearTemp();
        // 初始化服务管理器
        serviceManager.init(callback);
        log.info("application initialized");
        initFlag = true;
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

    public SpiderTemplate getSpiderTemplate() {
        ClientInfo clientInfo = clientManager.getCurrentClientImmediately();

        if (clientInfo == null) {
            throw new IllegalStateException("no client connected");
        }

        return spiderTemplateMap.get(clientInfo.getClientType());
    }

    public void destroy() {
        serviceManager.destroy();
        AsyncUtil.destroy();
        kebSocketTopicKeeper.destroy();
        if (initFlag) {
            router.getPrimary().close();
            if (CollectionUtil.isNotEmpty(spiderTemplateMap)) {
                spiderTemplateMap.values().forEach(SpiderTemplate::destroy);
            }
        }
        singleInstanceApplicationHandler.releaseLock();
        System.exit(0);
    }
}
