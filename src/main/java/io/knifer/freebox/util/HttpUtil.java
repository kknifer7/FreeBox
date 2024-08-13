package io.knifer.freebox.util;

import com.google.common.base.Strings;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.Notifications;

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
                    HttpRequest.newBuilder().GET().uri(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Notifications.create().text(e.getMessage()).showError();

            return StringUtils.EMPTY;
        }
    }
}
