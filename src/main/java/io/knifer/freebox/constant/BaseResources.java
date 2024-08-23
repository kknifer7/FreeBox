package io.knifer.freebox.constant;

import com.google.common.io.Resources;
import javafx.scene.image.Image;
import lombok.experimental.UtilityClass;

/**
 * 资源
 *
 * @author Knifer
 */
@UtilityClass
public class BaseResources {

    public static final Image LOGO = new Image(Resources.getResource("image/logo.png").toString());

    public static final Image PICTURE_PLACEHOLDER = new Image(
            Resources.getResource("image/picture_placeholder.png").toString()
    );
}
