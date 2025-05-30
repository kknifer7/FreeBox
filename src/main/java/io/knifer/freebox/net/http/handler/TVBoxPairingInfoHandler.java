package io.knifer.freebox.net.http.handler;

import cn.hutool.http.HttpStatus;
import com.sun.net.httpserver.HttpExchange;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.model.s2c.PairingInfo;
import io.knifer.freebox.util.NetworkUtil;
import io.knifer.freebox.util.json.GsonUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Collection;

/**
 * tvbox配对信息服务处理
 *
 * @author Knifer
 */
public class TVBoxPairingInfoHandler implements HttpHandler {

    @Override
    public boolean support(HttpExchange httpExchange) {
        return BaseValues.HTTP_GET.equalsIgnoreCase(httpExchange.getRequestMethod()) &&
                httpExchange.getRequestURI().getPath().equals("/pairing/tvbox");
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try (httpExchange) {
            Collection<Pair<NetworkInterface, String>> networkInterfaces =
                    NetworkUtil.getAvailableNetworkInterfaceAndIPv4();
            String ip = networkInterfaces.isEmpty() ?
                    BaseValues.ANY_LOCAL_IP : networkInterfaces.iterator().next().getRight();
            String respData = GsonUtil.toJson(PairingInfo.from(ip, ConfigHelper.getWsPort()));

            httpExchange.sendResponseHeaders(HttpStatus.HTTP_OK, respData.length());
            httpExchange.getResponseBody().write(respData.getBytes());
        } catch (Exception e) {
            try {
                httpExchange.sendResponseHeaders(HttpStatus.HTTP_INTERNAL_ERROR, -1);
            } catch (IOException ignored) {}
        }
    }
}
