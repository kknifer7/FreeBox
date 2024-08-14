package io.knifer.freebox.util;

import lombok.experimental.UtilityClass;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * 验证工具类
 *
 * @author Knifer
 */
@UtilityClass
public class ValidationUtil {

    public boolean isURL(String cs) {
        try {
            new URL(cs).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }

        return true;
    }
}
