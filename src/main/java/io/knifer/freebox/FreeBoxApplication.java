package io.knifer.freebox;

import com.sun.net.httpserver.HttpServer;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.net.http.FreeBoxHttpHandler;
import io.knifer.freebox.net.websocket.FreeBoxWebSocketServer;
import javafx.application.Application;
import javafx.application.Platform;
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

    private static HttpServer httpServer;

    static {
        try {
            httpServer = HttpServer.create(
                    new InetSocketAddress("192.168.0.13", 9897), 0
            );
            httpServer.createContext("/", new FreeBoxHttpHandler());
            httpServer.setExecutor(null);
        } catch (IOException e) {
            httpServer = null;
            log.error("failed to start http service, ", e);
            Platform.exit();
        }
    }

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
        Context.INSTANCE.init(this);
    }

    @Override
    public void stop() {
        stopHttpService();
        stopWSService();
    }

    private void stopHttpService() {
        log.info("Stopping HTTP Service......");
        if (httpServer == null) {
            return;
        }
        httpServer.stop(0);
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
        new Thread(() -> {
            httpServer.start();
            log.info("HTTP Service start successfully.");
        }).start();
        launch();
    }
}