package io.knifer.freebox.helper;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.util.CastUtil;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.stage.Stage;
import lombok.experimental.UtilityClass;

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

    public void route(Stage currentStage, Stage nextStage) {
        Context.INSTANCE.pushStage(currentStage);
        Context.INSTANCE.setCurrentStage(nextStage);
        currentStage.hide();
        nextStage.show();
    }
}
