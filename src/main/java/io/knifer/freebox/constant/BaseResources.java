package io.knifer.freebox.constant;

import com.google.common.io.Resources;
import io.knifer.freebox.FreeBoxApplication;
import javafx.scene.image.Image;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * 资源
 *
 * @author Knifer
 */
@Slf4j
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

    public static final Image PLAY_BUTTON_IMG = new Image(
            FreeBoxApplication.class.getResourceAsStream("image/play_button.png")
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

    @NotNull
    public static final URL PLAYER_CSS = FreeBoxApplication.class.getResource("css/player.css");
    @NotNull
    public static final URL LOADING_DIALOG_CSS =
            FreeBoxApplication.class.getResource("css/dialog/loading-dialog.css");

    static {
        if (PLAYER_CSS == null) {
            log.error("player.css not found");
            System.exit(-1);
        }
        if (LOADING_DIALOG_CSS == null) {
            log.error("loading-dialog.css not found");
            System.exit(-1);
        }
    }
}
