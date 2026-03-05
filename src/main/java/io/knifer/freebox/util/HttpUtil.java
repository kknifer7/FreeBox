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
import java.util.concurrent.CompletableFuture;

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
        String[] protocolAndPath = url.split("://", 2);
        String protocol = protocolAndPath[0];
        String[] hostAndPath = protocolAndPath[1].split("/", 2);
        String host = hostAndPath[0];
        StringBuilder urlBuilder = new StringBuilder(protocol)
                .append("://")
                .append(IDN.toASCII(host))
                .append("/");
        String path;
        String[] pathAndParams;

        if (hostAndPath.length > 1) {
            path = hostAndPath[1];
            pathAndParams = path.split("\\?", 2);
            if (pathAndParams.length > 1) {
                urlBuilder.append(URLEncodeUtil.encode(pathAndParams[0]))
                        .append("?")
                        .append(URLEncodeUtil.encode(pathAndParams[1]));
            } else {
                urlBuilder.append(URLEncodeUtil.encode(path));
            }
        }

        return urlBuilder.toString();
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
