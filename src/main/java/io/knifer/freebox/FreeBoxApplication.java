package io.knifer.freebox;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.component.node.SplashScreen;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.Views;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.ioc.IOC;
import io.knifer.freebox.service.LoadConfigService;
import io.knifer.freebox.util.FXMLUtil;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FreeBoxApplication extends Application {

    private SplashScreen splashScreen;
    private Context context;

    @Override
    public void start(Stage stage) throws IOException {
        LoadConfigService loadConfigService;
        Logger logger;

        try {
            // 启动画面
            splashScreen = new SplashScreen(0.8);
            splashScreen.show();
            loadConfigService = new LoadConfigService();
            loadConfigService.setOnSucceeded(event -> {
                // 初始化IOC容器和上下文
                IOC.init(stage);
                context = IOC.getBean(Context.class);
                context.init(this, () -> {
                    FXMLUtil.load(Views.HOME, stage);
                    stage.setTitle("FreeBox");
                    stage.show();
                    context.postEvent(AppEvents.APP_INITIALIZED);
                    closeSplashScreen();
                });
            });
            loadConfigService.start();
        } catch (Exception e) {
            logger = LoggerFactory.getLogger(FreeBoxApplication.class);
            logger.error("app start failed");
            closeSplashScreen();
            ToastHelper.showException(e);
        }
    }

    @Override
    public void stop() {
        closeSplashScreen();
        if (context != null) {
            context.destroy();
        }
    }

    private void closeSplashScreen() {
        if (splashScreen != null) {
            splashScreen.close();
            splashScreen = null;
        }
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler.getInstance());
        launch();
    }
}