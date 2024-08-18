package io.knifer.freebox;

import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ResourceBundle;

@Slf4j
public class FreeBoxApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                FreeBoxApplication.class.getResource("home-view.fxml"),
                ResourceBundle.getBundle("i18n.chs")
        );
        Scene scene = new Scene(fxmlLoader.load(), 960, 720);

        stage.setTitle("FreeBox");
        stage.setScene(scene);
        stage.getIcons().add(BaseResources.LOGO);
        stage.show();

        // 初始化上下文
        Context.INSTANCE.init(
                this,
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