package io.knifer.freebox.net.http.handler;

import cn.hutool.http.HttpStatus;
import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.HttpExchange;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ts分片代理
 * 用于ts分片有非标准文件头的情况
 * @author Knifer
 */
@Slf4j
public class ProxyTsHandler implements HttpHandler {

    private static final byte[] WRONG_HEADER = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };

    @Override
    public boolean support(HttpExchange httpExchange) {
        return BaseValues.HTTP_GET.equalsIgnoreCase(httpExchange.getRequestMethod()) &&
                httpExchange.getRequestURI().getPath().startsWith("/proxy/ts");
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        String tsUrl = StringUtils.substringAfter(httpExchange.getRequestURI().getPath(), "/proxy/ts/");
        byte[] data = ValidationUtil.isURL(tsUrl) ? fixTSHeader(tsUrl) : ArrayUtils.EMPTY_BYTE_ARRAY;

        try (httpExchange) {
            httpExchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "video/MP2T");
            httpExchange.sendResponseHeaders(HttpStatus.HTTP_OK, data.length);
            httpExchange.getResponseBody().write(fixTSHeader(tsUrl));
        } catch (Exception e) {
            log.warn("send response failed", e);
        }
    }

    private byte[] fixTSHeader(String tsUrl) {
        HttpRequest request;
        byte[] data;
        boolean needFix;
        byte[] fixedData;

        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(tsUrl))
                    .header(HttpHeaders.USER_AGENT, BaseValues.USER_AGENT)
                    .header("Accept", "*/*")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            data = HttpUtil.getClient()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray())
                    .body();
        } catch (Exception e) {
            log.info("fetch ts content failed", e);

            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        if (data == null) {

            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        if (data.length >= 8) {
            needFix = true;
            for (int i = 0; i < 8; i++) {
                if (data[i] != WRONG_HEADER[i]) {
                    needFix = false;
                    break;
                }
            }
            if (needFix) {
                fixedData = new byte[data.length - 8];
                System.arraycopy(data, 8, fixedData, 0, fixedData.length);

                return fixedData;
            }
        }

        return data;
    }
}
