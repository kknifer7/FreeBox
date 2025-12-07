package io.knifer.freebox.component.node;

import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

import javax.swing.*;

/**
 * 加载等待窗口
 *
 * @author Knifer
 */
public class LoadingDialog extends AbstractNotificationDialog<Void> {

    private int closeButtonAvailableTimeCount = CANCEL_BUTTON_DISABLE_SECONDS;

    private static final StackPane INDICATOR;
    private static final String CANCEL_BUTTON_TEXT = I18nHelper.get(I18nKeys.COMMON_CANCEL) + " (%s)";
    private static final int CANCEL_BUTTON_DISABLE_SECONDS = 8;

    static {
        ProgressIndicator pi = new ProgressIndicator();

        pi.getStyleClass().add("loading-indicator");
        pi.setPrefSize(40, 40);
        INDICATOR = new StackPane(pi);
    }

    public LoadingDialog() {
        super();

        DialogPane dialogPane = getDialogPane();
        VBox contentBox;
        Button cancelButton;
        Timer closeButtonAvailableTimer;

        dialogPane.getStylesheets().add(
                BaseResources.LOADING_DIALOG_CSS.toExternalForm()
        );
        dialogPane.getStyleClass().add("loading-dialog");
        contentBox = new VBox();
        contentBox.getStyleClass().add("loading-content");
        contentBox.getChildren().add(INDICATOR);
        dialogPane.setContent(contentBox);
        dialogPane.getButtonTypes().add(ButtonType.CANCEL);
        cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.setDisable(true);
        cancelButton.setText(String.format(CANCEL_BUTTON_TEXT, closeButtonAvailableTimeCount));
        cancelButton.getStyleClass().add("cancel-button");
        closeButtonAvailableTimer = new Timer(1000, e -> {
            closeButtonAvailableTimeCount--;
            if (closeButtonAvailableTimeCount <= 0) {
                closeButtonAvailableTimeCount = CANCEL_BUTTON_DISABLE_SECONDS;
                ((Timer) e.getSource()).stop();
                Platform.runLater(() -> {
                    cancelButton.setDisable(false);
                    cancelButton.setText(I18nHelper.get(I18nKeys.COMMON_CANCEL));
                });

                return;
            }
            Platform.runLater(
                    () -> cancelButton.setText(String.format(CANCEL_BUTTON_TEXT, closeButtonAvailableTimeCount))
            );
        });
        initStyle(StageStyle.UNDECORATED);
        setOnHiding(e -> {
            closeButtonAvailableTimer.stop();
            closeButtonAvailableTimeCount = CANCEL_BUTTON_DISABLE_SECONDS;
        });
        closeButtonAvailableTimer.start();
    }
}