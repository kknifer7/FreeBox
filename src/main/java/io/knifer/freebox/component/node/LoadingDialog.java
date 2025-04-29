package io.knifer.freebox.component.node;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.StageStyle;

/**
 * 加载等待窗口
 *
 * @author Knifer
 */
public class LoadingDialog extends AbstractNotificationDialog<Void> {

    private static final StackPane INDICATOR;

    static {
        ProgressIndicator pi = new ProgressIndicator();

        pi.setPrefSize(30, 30);
        INDICATOR = new StackPane(pi);
    }

    public LoadingDialog() {
        super();
        initStyle(StageStyle.UNDECORATED);
        getDialogPane().contentProperty().set(INDICATOR);
    }
}
