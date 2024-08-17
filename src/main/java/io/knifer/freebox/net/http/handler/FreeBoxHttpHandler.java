package io.knifer.freebox.net.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.model.s2c.PairingInfo;
import io.knifer.freebox.util.GsonUtil;

import java.io.IOException;
import java.io.OutputStream;

/**
 * HTTP服务处理
 *
 * @author Knifer
 */
public class FreeBoxHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        OutputStream out;
        byte[] respData;

        if (canHandle(exchange)) {
            out = exchange.getResponseBody();
            respData = GsonUtil.toJson(
                    PairingInfo.from(ConfigHelper.getServiceIPv4(), ConfigHelper.getWsPort())
            ).getBytes();
            exchange.sendResponseHeaders(200, respData.length);
            out.write(respData);
        } else {
            exchange.sendResponseHeaders(403, 0);
        }
        exchange.close();
    }

    private boolean canHandle(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod()) &&
                exchange.getRequestURI().getPath().equals("/pairing/tvbox");
    }
}
