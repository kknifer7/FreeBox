package io.knifer.freebox.util;

import io.knifer.freebox.exception.FBException;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP
 *
 * @author Knifer
 */
@UtilityClass
public class HttpUtil {

    private final static HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();


    public String get(String url) {
        try {
            return client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(url))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();
        } catch (IOException e) {
            throw new FBException("Error while sending request to " + url, e);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public byte[] getFile(String url) {
        try {
            return client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(url))
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            ).body();
        } catch (IOException e) {
            throw new FBException("Error while sending request to " + url, e);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public CompletableFuture<String> getAsync(String url) {
        return client.sendAsync(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(url))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(HttpResponse::body);
    }

    public String get(String url, String... headers) {
        try {
            return client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(url))
                            .headers(headers)
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();
        } catch (IOException e) {
            throw new FBException("Error while sending request to " + url, e);
        } catch (InterruptedException ignored) {
            return null;
        }
    }

    public String post(String url, String body) {
        try {
            return client.send(
                    HttpRequest.newBuilder()
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .uri(URI.create(url))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();
        } catch (IOException e) {
            throw new FBException("Error while sending request to " + url, e);
        } catch (InterruptedException e) {

           return null;
        }
    }
}
