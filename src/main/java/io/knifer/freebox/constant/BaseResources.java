package io.knifer.freebox.constant;

import cn.hutool.setting.Setting;
import com.google.common.io.Resources;
import io.knifer.freebox.FreeBoxApplication;
import javafx.scene.image.Image;
import lombok.experimental.UtilityClass;

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

    public static final Setting X_PROPERTIES = new Setting(
            Resources.getResource("x.properties").getPath()
    );
}
