package io.knifer.freebox.helper;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.util.CastUtil;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

/**
 * 窗口
 *
 * @author Knifer
 */
@UtilityClass
public class WindowHelper {

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

    public <T> T getUserData(Node node) {
        return CastUtil.cast(getStage(node).getUserData());
    }

    public Stage getStage(Node node) {
        return CastUtil.cast(node.getScene().getWindow());
    }

    public void setFontFamily(Node node, String fontFamily) {
        setFontFamily(getStage(node), fontFamily);
    }

    public void setFontFamily(Window window, String fontFamily) {
        Scene scene = window.getScene();
        Parent parent = scene.getRoot();
        String style = parent.getStyle();
        String oldFontFamily = StringUtils.substringBetween(style, "-fx-font-family:", ";");

        if (style.isEmpty() || oldFontFamily == null) {
            parent.setStyle(style + "-fx-font-family:" + fontFamily + ";");
        } else if (!oldFontFamily.equals(fontFamily)) {
            parent.setStyle(style.replace(oldFontFamily, fontFamily));
        }
    }

    public void route(Stage currentStage, Stage nextStage) {
        Context.INSTANCE.pushStage(currentStage);
        Context.INSTANCE.setCurrentStage(nextStage);
        currentStage.hide();
        nextStage.show();
    }
}
