package io.knifer.freebox;

import io.knifer.freebox.component.node.SplashScreen;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.Views;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.util.FXMLUtil;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class FreeBoxApplication extends Application {

    private SplashScreen splashScreen;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            // 启动画面
            splashScreen = new SplashScreen(0.8);
            splashScreen.show();
            // 初始化上下文
            Context.INSTANCE.init(
                    this,
                    stage,
                    () -> {
                        FXMLUtil.load(Views.HOME, stage);
                        stage.setTitle("FreeBox");
                        stage.show();
                        Context.INSTANCE.postEvent(AppEvents.APP_INITIALIZED);
                        closeSplashScreen();
                    }
            );
        } catch (Exception e) {
            log.error("app start failed", e);
            closeSplashScreen();
            ToastHelper.showException(e);
        }
    }

    @Override
    public void stop() {
        closeSplashScreen();
        Context.INSTANCE.destroy();
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