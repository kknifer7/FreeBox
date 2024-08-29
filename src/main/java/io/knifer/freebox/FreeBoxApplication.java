package io.knifer.freebox;

import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.Views;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.util.FXMLUtil;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class FreeBoxApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLUtil.load(Views.HOME, stage);

        stage.setTitle("FreeBox");
        stage.show();

        // 初始化上下文
        Context.INSTANCE.init(
                this,
                stage,
                () -> Context.INSTANCE.postEvent(AppEvents.APP_INITIALIZED)
        );
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