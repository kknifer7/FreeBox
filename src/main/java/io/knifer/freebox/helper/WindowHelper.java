package io.knifer.freebox.helper;

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

    public void close(Node node) {
        ((Stage) node.getScene().getWindow()).close();
    }
}
