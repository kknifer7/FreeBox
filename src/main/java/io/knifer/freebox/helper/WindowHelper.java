package io.knifer.freebox.helper;

import io.knifer.freebox.util.CastUtil;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.experimental.UtilityClass;

/**
 * 窗口
 *
 * @author Knifer
 */
@UtilityClass
public class WindowHelper {

    private static final String FONT_FAMILY_STYLE_KEY = "-fx-font-family:";

    private static String cssFontFamily(String fontFamily) {
        return "\"" + fontFamily + "\"";
    }

    public void close(Event event) {
        if (event.getTarget() instanceof Node n) {
            close(n);
        } else if (event.getSource() instanceof Node n) {
            close(n);
        }
    }

    public void close(Node node) {
        getStage(node).close();
    }

    public void hide(Event event) {
        if (event.getTarget() instanceof Node n) {
            hide(n);
        } else if (event.getSource() instanceof Node n) {
            hide(n);
        }
    }

    public void hide(Node node) {
        getStage(node).hide();
    }

    public Stage getStage(Node node) {
        return CastUtil.cast(node.getScene().getWindow());
    }

    public void setFontFamily(Node node, String fontFamily) {
        setFontFamily(getStage(node), fontFamily);
    }

    public void setFontFamily(Window window, String fontFamily) {
        Scene scene = window.getScene();

        if (scene == null) {

            return;
        }
        applyFontFamily(scene.getRoot(), fontFamily);
    }

    private static void applyFontFamily(Parent root, String fontFamily) {
        String existingStyle = root.getStyle();
        String existingFontFamily = extractFontFamily(existingStyle);
        String cssValue = cssFontFamily(fontFamily);

        if (existingFontFamily == null) {
            root.setStyle(existingStyle + FONT_FAMILY_STYLE_KEY + cssValue + ";");
        } else if (!existingFontFamily.equals(fontFamily)) {
            root.setStyle(existingStyle.replace(
                    FONT_FAMILY_STYLE_KEY + cssFontFamily(existingFontFamily),
                    FONT_FAMILY_STYLE_KEY + cssValue
            ));
        }
    }

    private static String extractFontFamily(String style) {
        int prefixIdx = style.indexOf(FONT_FAMILY_STYLE_KEY);

        if (prefixIdx < 0) {

            return null;
        }
        int valueStart = prefixIdx + FONT_FAMILY_STYLE_KEY.length();
        String rawValue = style.substring(valueStart, style.indexOf(";", valueStart));

        return rawValue.startsWith("\"") && rawValue.endsWith("\"")
                ? rawValue.substring(1, rawValue.length() - 1)
                : rawValue;
    }
}
