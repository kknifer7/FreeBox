package io.knifer.freebox.util;

import io.knifer.freebox.exception.FBException;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * HTTP
 *
 * @author Knifer
 */
@UtilityClass
public class HttpUtil {

    private final static HttpClient client = HttpClient.newHttpClient();


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
            e.printStackTrace();
            throw new FBException("Error while sending request to " + url, e);
        } catch (InterruptedException e) {
            return null;
        }
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
            e.printStackTrace();
            throw new FBException("Error while sending request to " + url, e);
        } catch (InterruptedException ignored) {
            return null;
        }
    }
}
