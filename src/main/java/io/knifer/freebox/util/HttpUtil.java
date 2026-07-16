package io.knifer.freebox.util;

import cn.hutool.core.net.URLEncodeUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.IDN;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * HTTP
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class HttpUtil {

    private final static Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    @Getter
    private final static OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(DEFAULT_TIMEOUT)
            .readTimeout(DEFAULT_TIMEOUT)
            .writeTimeout(DEFAULT_TIMEOUT)
            .build();

    public String get(String url, String... headers) {
        try (
                Response response = client.newCall(
                        new Request.Builder()
                                .url(parseUrl(url))
                                .headers(Headers.of(headers))
                                .build()
                ).execute()
        ){
            return response.body().string();
        } catch (Exception e) {
            log.error("get error", e);

            return StringUtils.EMPTY;
        }
    }

    public byte[] getFile(String url) throws IOException {
        try (
                Response response = client.newCall(
                        new Request.Builder()
                                .url(parseUrl(url))
                                .build()
                ).execute()
        ){

            return response.body().bytes();
        } catch (Exception e) {
            log.error("getFile error", e);

            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
    }

    public byte[] getFile(String url, String... headers) {
        try (
                Response response = client.newCall(
                        new Request.Builder()
                                .url(parseUrl(url))
                                .headers(Headers.of(headers))
                                .build()
                ).execute()
        ){

            return response.body().bytes();
        } catch (Exception e) {
            log.error("getFile error", e);

            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
    }

    public CompletableFuture<String> getAsync(String url) {
        CompletableFuture<String> result = new CompletableFuture<>();

        client.newCall(
                new Request.Builder()
                        .url(parseUrl(url))
                        .build()
        ).enqueue(new StringResponseCallback(result));

        return result;
    }

    public CompletableFuture<String> getAsync(String url, String... headers) {
        CompletableFuture<String> result = new CompletableFuture<>();

        client.newCall(
                new Request.Builder()
                        .url(parseUrl(url))
                        .headers(Headers.of(headers))
                        .build()
        ).enqueue(new StringResponseCallback(result));

        return result;
    }

    public String parseUrl(String url) {
        String[] protocolAndPath;
        String[] hostAndPath;
        String protocol;
        String host;
        StringBuilder urlBuilder;

        if (StringUtils.isBlank(url)) {
            return StringUtils.EMPTY;
        }
        protocolAndPath = url.split("://", 2);
        if (protocolAndPath.length < 2) {
            log.warn("invalid url: {}", url);

            return url;
        }
        protocol = protocolAndPath[0];
        hostAndPath = protocolAndPath[1].split("/", 2);
        try {
            host = IDN.toASCII(hostAndPath[0]);
        } catch (Exception e) {
            log.warn("invalid host: {}", hostAndPath[0], e);

            return url;
        }
        urlBuilder = new StringBuilder(protocol)
                .append("://")
                .append(host)
                .append("/");
        if (hostAndPath.length > 1) {
            appendEncodedPathAndQuery(urlBuilder, hostAndPath[1]);
        }

        return urlBuilder.toString();
    }

    private void appendEncodedPathAndQuery(StringBuilder urlBuilder, String pathAndQuery) {
        String[] pathAndParams;

        pathAndParams = pathAndQuery.split("\\?", 2);
        urlBuilder.append(encodePath(pathAndParams[0]));
        if (pathAndParams.length > 1) {
            urlBuilder.append("?").append(encodeQuery(pathAndParams[1]));
        }
    }

    private String encodePath(String path) {
        return Arrays.stream(path.split("/"))
                .map(segment -> URLEncodeUtil.encode(segment).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }

    private String encodeQuery(String query) {
        return Arrays.stream(query.split("&"))
                .map(pair -> {
                    String[] kv = pair.split("=", 2);
                    String key = URLEncodeUtil.encode(kv[0]);

                    if (kv.length > 1) {
                        return key + "=" + URLEncodeUtil.encode(kv[1]);
                    }

                    return key;
                })
                .collect(Collectors.joining("&"));
    }

    @AllArgsConstructor
    private static class StringResponseCallback implements Callback {

        private final CompletableFuture<String> future;

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            log.error("getAsync request failure", e);
            future.complete(StringUtils.EMPTY);
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) {
            try (response) {
                future.complete(response.body().string());
            } catch (IOException e) {
                log.error("getAsync request read error", e);
                future.complete(StringUtils.EMPTY);
            }
        }
    }
}
