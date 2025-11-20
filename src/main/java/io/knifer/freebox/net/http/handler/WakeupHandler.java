package io.knifer.freebox.net.http.handler;

import cn.hutool.http.HttpStatus;
import com.sun.net.httpserver.HttpExchange;
import io.knifer.freebox.constant.BaseValues;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

/**
 * 应用唤起处理器
 *
 * @author Knifer
 */
@Slf4j
public class WakeupHandler implements HttpHandler {

    @Override
    public boolean support(HttpExchange httpExchange) {
        return BaseValues.HTTP_GET.equalsIgnoreCase(httpExchange.getRequestMethod()) &&
                httpExchange.getRequestURI().getPath().equals("/wakeup");
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        byte[] respData = "ok".getBytes();

        log.info("user try starting another application instance, wakeup exists application");
        Platform.runLater(() -> Window.getWindows().forEach(window -> {
            if (window instanceof Stage stage) {
                // 唤起窗口
                stage.toFront();
                stage.setAlwaysOnTop(true);
                stage.setAlwaysOnTop(false);
            }
        }));
        try (httpExchange){
            httpExchange.sendResponseHeaders(HttpStatus.HTTP_OK, respData.length);
            httpExchange.getResponseBody().write(respData);
        } catch (Exception e) {
            log.error("handle /wakeup failed", e);
        }
    }
}
