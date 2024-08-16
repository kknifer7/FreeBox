package io.knifer.freebox;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.net.http.FreeBoxHttpServer;
import io.knifer.freebox.net.websocket.FreeBoxWebSocketServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ResourceBundle;

@Slf4j
public class FreeBoxApplication extends Application {

    private static final WebSocketServer wsServer =
            new FreeBoxWebSocketServer(new InetSocketAddress("192.168.0.13", 9898));
    private static final Thread wsThread = new Thread(wsServer);

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                FreeBoxApplication.class.getResource("home-view.fxml"),
                ResourceBundle.getBundle("i18n.chs")
        );
        Scene scene = new Scene(fxmlLoader.load(), 960, 720);

        stage.setTitle("FreeBox");
        stage.setScene(scene);
        stage.show();

        // 初始化上下文
        Context.INSTANCE.init(this, new FreeBoxHttpServer());
    }

    @Override
    public void stop() {
        Context.INSTANCE.destroy();
        stopWSService();
    }

    private void stopWSService() {
        log.info("Stopping WebSocket Service......");
        try {
            wsServer.stop(5);
        } catch (InterruptedException ignored) {
            wsThread.interrupt();
        }
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
        wsThread.start();
        launch();
    }
}