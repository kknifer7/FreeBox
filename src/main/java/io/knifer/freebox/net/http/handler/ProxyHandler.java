package io.knifer.freebox.net.http.handler;

import cn.hutool.http.HttpStatus;
import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.HttpExchange;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.spider.SpiderJarLoader;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代理服务检测处理
 *
 * @author Knifer
 */
@Slf4j
public class ProxyHandler implements HttpHandler {

    @Override
    public boolean support(HttpExchange httpExchange) {
        return BaseValues.HTTP_GET.equalsIgnoreCase(httpExchange.getRequestMethod()) &&
                httpExchange.getRequestURI().getPath().equals("/proxy");
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try (httpExchange) {
            Map<String, String> parameterMap = parseParameterMap(httpExchange.getRequestURI().getQuery());
            Object[] proxyInvokeResult = SpiderJarLoader.getInstance().proxyInvoke(parameterMap);
            String url;
            byte[] respBytes;
            int code;

            log.info("spider proxyInvoke result: {}", proxyInvokeResult);
            if (ArrayUtils.isEmpty(proxyInvokeResult)) {
                httpExchange.sendResponseHeaders(HttpStatus.HTTP_INTERNAL_ERROR, -1);
            } else if (proxyInvokeResult[0] instanceof Response proxyResp) {
                try (proxyResp) {
                    url = proxyResp.request().url().toString();
                    respBytes = url.getBytes();
                    httpExchange.sendResponseHeaders(HttpStatus.HTTP_OK, respBytes.length);
                    httpExchange.getResponseBody().write(respBytes);
                }
            } else {
                try {
                    code = (int) proxyInvokeResult[0];
                    if (code > 299 && code < 400) {
                        httpExchange.sendResponseHeaders(code, -1);
                        httpExchange.getResponseHeaders().add(HttpHeaders.LOCATION, (String) proxyInvokeResult[2]);

                        return;
                    }
                } catch (ClassCastException ignored) {}
                if (proxyInvokeResult[2] instanceof InputStream input) {
                    try {
                        code = (int) proxyInvokeResult[0];
                        if (proxyInvokeResult.length == 4) {
                            if (proxyInvokeResult[3] instanceof Map<?,?> headers) {
                                for (Map.Entry<?, ?> entry : headers.entrySet()) {
                                    httpExchange.getResponseHeaders().put(
                                            entry.getKey().toString(), List.of(entry.getValue().toString())
                                    );
                                }
                            }
                        }
                        httpExchange.sendResponseHeaders(code, 0);
                        input.transferTo(httpExchange.getResponseBody());

                        return;
                    } catch (ClassCastException ignored) {}
                }
                httpExchange.sendResponseHeaders(HttpStatus.HTTP_INTERNAL_ERROR, -1);
            }
        } catch (Exception e) {
            try {
                httpExchange.sendResponseHeaders(HttpStatus.HTTP_INTERNAL_ERROR, -1);
            } catch (IOException ignored) {}
        }
    }

    private Map<String, String> parseParameterMap(String parameters) {
        Map<String, String> result;
        String[] entry;

        if (StringUtils.isBlank(parameters)) {
            return Map.of();
        }
        result = new HashMap<>();
        for (String param : parameters.split("&")) {
            entry = param.split("=");
            if (entry.length > 1) {
                result.put(
                        URLDecoder.decode(entry[0], Charsets.UTF_8),
                        URLDecoder.decode(entry[1], Charsets.UTF_8)
                );
            } else {
                result.put(
                        URLDecoder.decode(entry[0], Charsets.UTF_8),
                        StringUtils.EMPTY
                );
            }
        }

        return result;
    }
}
