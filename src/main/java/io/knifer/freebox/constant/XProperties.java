package io.knifer.freebox.constant;

import com.google.common.io.Resources;
import lombok.experimental.UtilityClass;

import java.io.InputStream;
import java.util.Properties;

/**
 * x.properties 配置
 *
 * @author Knifer
 */
@UtilityClass
public class XProperties {

    public static final Properties INSTANCE = new Properties() {
        {
            try (InputStream in = Resources.getResource("x.properties").openStream()) {
                load(in);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };
}
