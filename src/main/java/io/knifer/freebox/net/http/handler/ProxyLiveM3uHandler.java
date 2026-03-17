package io.knifer.freebox.net.http.handler;

import cn.hutool.core.net.Ipv4Util;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.HttpExchange;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.util.ValidationUtil;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 直播m3u代理
 *
 * @author Knifer
 */
@Slf4j
@Singleton
public class ProxyLiveM3uHandler implements HttpHandler {

    private static final OkHttpClient httpClient = io.knifer.freebox.util.HttpUtil.getClient();

    @Override
    public boolean support(HttpExchange httpExchange) {
        return BaseValues.HTTP_GET.equalsIgnoreCase(httpExchange.getRequestMethod()) &&
                Ipv4Util.isInnerIP(httpExchange.getLocalAddress().getHostString()) &&
                ValidationUtil.isURL(StringUtils.substringAfter(
                        httpExchange.getRequestURI().getPath(), "/proxy-live/"
                ));
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try (httpExchange) {
            URI requestURI = httpExchange.getRequestURI();
            Map<String, String> paramMap = HttpUtil.decodeParamMap(requestURI.getQuery(), Charsets.UTF_8);
            String url = StringUtils.substringAfter(requestURI.toString(), "/proxy-live/");
            String ua = paramMap.get(BaseValues.LIVE_M3U_ATTR_HTTP_USER_AGENT);
            String referer = paramMap.get(BaseValues.LIVE_M3U_ATTR_HTTP_REFERER);
            String header = paramMap.get(BaseValues.LIVE_M3U_ATTR_HTTP_HEADER);
            Request.Builder reqBuilder = new Request.Builder()
                    .url(url);
            String[] otherHeaders;
            String[] headerKV;
            Request req;
            String originUrl;

            if (StringUtils.isNotBlank(ua)) {
                reqBuilder.header(HttpHeaders.USER_AGENT, ua);
            }
            if (StringUtils.isNotBlank(referer)) {
                reqBuilder.header(HttpHeaders.REFERER, referer);
            }
            if (StringUtils.isNotBlank(header)) {
                otherHeaders = header.split("&");
                if (otherHeaders.length > 0) {
                    if (otherHeaders.length == 1) {
                        headerKV = otherHeaders[0].split("=");
                        reqBuilder.header(headerKV[0], headerKV[1]);
                    } else if (otherHeaders.length % 2 == 0) {
                        for (String h : otherHeaders) {
                            headerKV = h.split("=");
                            if (headerKV.length == 2) {
                                reqBuilder.header(headerKV[0], headerKV[1]);
                            }
                        }
                    }
                }
            }
            req = reqBuilder.build();
            try (Response response = httpClient.newCall(req).execute()) {
                originUrl = response.request().url().toString();
                log.info("process url : {}, origin live url: {}", url, originUrl);
                httpExchange.getResponseHeaders()
                        .put(HttpHeaders.LOCATION, List.of(originUrl));
                httpExchange.sendResponseHeaders(HttpStatus.HTTP_MOVED_TEMP, -1);
            } catch (IOException ignored) {} catch (Exception e) {
                log.warn("proxy live m3u failed", e);
                try {
                    httpExchange.sendResponseHeaders(HttpStatus.HTTP_INTERNAL_ERROR, -1);
                } catch (IOException ignored) {}
            }
        }
    }
}
