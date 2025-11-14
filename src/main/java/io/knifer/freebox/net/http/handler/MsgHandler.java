package io.knifer.freebox.net.http.handler;

import cn.hutool.http.HttpStatus;
import com.sun.net.httpserver.HttpExchange;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.helper.ToastHelper;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 代理服务检测处理
 *
 * @author Knifer
 */
@Slf4j
public class MsgHandler implements HttpHandler {

    @Override
    public boolean support(HttpExchange httpExchange) {
        return BaseValues.HTTP_GET.equalsIgnoreCase(httpExchange.getRequestMethod()) && httpExchange.getRequestURI().getPath().equals("/postMsg");
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        byte[] respData = "ok".getBytes();
        String query = httpExchange.getRequestURI().getQuery();


        try (httpExchange) {
            httpExchange.sendResponseHeaders(HttpStatus.HTTP_OK, respData.length);
            httpExchange.getResponseBody().write(respData);
        } catch (Exception e) {
            log.error("handle /postMsg failed", e);
        } finally {
            Platform.runLater(()-> {
                if (StringUtils.contains(query, "msg=")) {
                    ToastHelper.showInfo(query.replace("msg=", ""));
                }
            });

        }
    }


}
