package io.knifer.freebox.constant;

import com.google.common.io.Resources;
import io.knifer.freebox.FreeBoxApplication;
import javafx.scene.image.Image;
import lombok.experimental.UtilityClass;

import java.io.InputStream;
import java.util.Properties;

/**
 * 资源
 *
 * @author Knifer
 */
@UtilityClass
@SuppressWarnings("ConstantConditions")
public class BaseResources {

    public static final Image LOGO_IMG = new Image(FreeBoxApplication.class.getResourceAsStream("image/logo.png"));

    public static final Image PICTURE_PLACEHOLDER_IMG = new Image(
            FreeBoxApplication.class.getResourceAsStream("image/picture_placeholder.png")
    );

    public static final Image LOAD_MORE_IMG = new Image(
            FreeBoxApplication.class.getResourceAsStream("image/load_more.png")
    );

    public static final Image PLAYING_GIF = new Image(
            FreeBoxApplication.class.getResourceAsStream("image/playing.gif")
    );

    public static final Properties X_PROPERTIES = new Properties() {
        {
            try (InputStream in = Resources.getResource("x.properties").openStream()) {
                load(in);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };
}
