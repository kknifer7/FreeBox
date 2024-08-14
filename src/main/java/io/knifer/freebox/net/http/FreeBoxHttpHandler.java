package io.knifer.freebox.net.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.knifer.freebox.model.common.PairingInfo;
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
                    PairingInfo.from("192.168.0.13", 9898)
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
