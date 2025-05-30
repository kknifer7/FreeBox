package io.knifer.freebox.net.http.handler;

import cn.hutool.http.HttpStatus;
import com.sun.net.httpserver.HttpExchange;
import io.knifer.freebox.constant.BaseValues;


/**
 * 代理服务处理
 *
 * @author Knifer
 */

public class ProxyCkHandler implements HttpHandler{

    @Override
    public boolean support(HttpExchange httpExchange) {
        return BaseValues.HTTP_GET.equalsIgnoreCase(httpExchange.getRequestMethod()) &&
                httpExchange.getRequestURI().getPath().equals("/proxy") &&
                httpExchange.getRequestURI().getQuery().equals("do=ck");
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        byte[] respData = "ok".getBytes();

        try (httpExchange) {
            httpExchange.sendResponseHeaders(HttpStatus.HTTP_OK, respData.length);
            httpExchange.getResponseBody().write(respData);
        } catch (Exception ignored) {}
    }
}
