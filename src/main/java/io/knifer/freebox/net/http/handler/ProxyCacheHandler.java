package io.knifer.freebox.net.http.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.http.HttpStatus;
import com.sun.net.httpserver.HttpExchange;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.CacheKeys;
import io.knifer.freebox.helper.CacheHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 用于本地代理获取缓存数据
 */
@Slf4j
public class ProxyCacheHandler implements HttpHandler{
    @Override
    public boolean support(HttpExchange httpExchange) {
        return BaseValues.HTTP_GET.equalsIgnoreCase(httpExchange.getRequestMethod()) &&
                Ipv4Util.isInnerIP(httpExchange.getLocalAddress().getHostString()) &&
                !StringUtils.substringAfter(httpExchange.getRequestURI().getPath(), "/proxy-cache/").isBlank();
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        String pathParam = StringUtils.substringAfter(httpExchange.getRequestURI().getPath(), "/proxy-cache/");
        String cacheVal = CacheHelper.get(pathParam);
        Map<String, List<String>> proxyRespHeaders;
        byte[] respData;

        try (httpExchange) {
            if (cacheVal == null) {
                httpExchange.sendResponseHeaders(HttpStatus.HTTP_NOT_FOUND, -1);
            } else {
                respData = cacheVal.getBytes();
                proxyRespHeaders = CacheHelper.get(CacheKeys.PROXY_CACHE_HTTP_HEADERS + pathParam);
                if (CollUtil.isNotEmpty(proxyRespHeaders)) {
                    httpExchange.getResponseHeaders().putAll(proxyRespHeaders);
                }
                httpExchange.sendResponseHeaders(HttpStatus.HTTP_OK, respData.length);
                httpExchange.getResponseBody().write(respData);
            }
        } catch (Exception e) {
            log.error("handle /proxy-cache failed, pathParam={}", pathParam, e);
        }
    }
}
