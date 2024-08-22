package io.knifer.freebox.helper;

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
        ((Stage) node.getScene().getWindow()).close();
    }

    public <T> T getUserData(Node node) {
        return CastUtil.cast(node.getScene().getWindow().getUserData());
    }
}
