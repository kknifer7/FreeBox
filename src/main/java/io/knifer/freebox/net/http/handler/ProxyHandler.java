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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            Map<String, String> parameterMap = parseParameterMap(httpExchange);
            Object[] proxyInvokeResult = SpiderJarLoader.getInstance().proxyInvoke(parameterMap);
            int code;

            log.info("parameters: {}, spider proxyInvoke result: {}", parameterMap, proxyInvokeResult);
            if (ArrayUtils.isEmpty(proxyInvokeResult)) {
                httpExchange.sendResponseHeaders(HttpStatus.HTTP_INTERNAL_ERROR, -1);
            } else if (proxyInvokeResult[0] instanceof Response proxyResp) {
                try (proxyResp) {
                    proxyResp.headers().forEach(nameValPair -> {
                        String name = nameValPair.getFirst();

                        if (HttpHeaders.CONTENT_DISPOSITION.equalsIgnoreCase(name)) {
                            // 移除可能导致错误的响应头
                            return;
                        }
                        httpExchange.getResponseHeaders().put(
                                name, List.of(nameValPair.getSecond())
                        );
                    });
                    httpExchange.sendResponseHeaders(proxyResp.code(), proxyResp.body().contentLength());
                    proxyResp.body().byteStream().transferTo(httpExchange.getResponseBody());
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

    private Map<String, String> parseParameterMap(HttpExchange exchange) {
        Map<String, String> result;
        String[] entry;
        String query = exchange.getRequestURI().getQuery();

        if (StringUtils.isBlank(query)) {
            return Map.of();
        }
        result = exchange.getRequestHeaders()
                .entrySet()
                .stream()
                .map(nameAndValue -> Map.entry(
                        nameAndValue.getKey(),
                        StringUtils.join(nameAndValue.getValue(), ",")
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (String param : query.split("&")) {
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
