package io.knifer.freebox;

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

    @Override
    public void start(Stage stage) throws IOException {
        try {
            // 初始化上下文
            Context.INSTANCE.init(
                    this,
                    stage,
                    () -> {
                        FXMLUtil.load(Views.HOME, stage);
                        stage.setTitle("FreeBox");
                        stage.show();
                        Context.INSTANCE.postEvent(AppEvents.APP_INITIALIZED);
                    }
            );
        } catch (Throwable e) {
            log.error("app start failed", e);
            ToastHelper.showException(e);
        }
    }

    @Override
    public void stop() {
        Context.INSTANCE.destroy();
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler.getInstance());
        launch();
    }
}