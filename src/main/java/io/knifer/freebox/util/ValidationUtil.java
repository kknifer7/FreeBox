package io.knifer.freebox.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

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
        if (cs == null || cs.isBlank()) {
            return false;
        }
        try {
            new URL(cs).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }

        return true;
    }

    public boolean isPort(String cs) {
        int port;

        if (!StringUtils.isNumeric(cs) || cs.length() > 5) {
            return false;
        }
        port = Integer.parseInt(cs);

        return isPort(port);
    }

    public boolean isPort(Integer num) {
        return num < 65536 && num > 0;
    }
}
