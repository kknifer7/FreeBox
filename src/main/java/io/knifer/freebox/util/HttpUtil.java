package io.knifer.freebox.util;

import cn.hutool.core.net.URLEncodeUtil;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.IDN;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP
 *
 * @author Knifer
 */
@UtilityClass
public class HttpUtil {

    @Getter
    private final static HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public byte[] getFile(String url) throws IOException {
        try {
            return client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(parseUrl(url))
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            ).body();
        } catch (InterruptedException ignored) {}

        return new byte[0];
    }

    public CompletableFuture<String> getAsync(String url) {
        return client.sendAsync(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(parseUrl(url))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(HttpResponse::body);
    }

    public CompletableFuture<String> getAsync(String url, String... headers) {
        return client.sendAsync(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(parseUrl(url))
                        .headers(headers)
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(HttpResponse::body);
    }

    public URI parseUrl(String url) {
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

        return URI.create(urlBuilder.toString());
    }
}
