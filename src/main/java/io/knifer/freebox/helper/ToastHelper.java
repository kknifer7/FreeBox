package io.knifer.freebox.helper;

import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.Notifications;
import org.controlsfx.dialog.ExceptionDialog;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

/**
 * 提示
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class ToastHelper {

    public void showSuccessI18n(String i18nKey) {
        showSuccess(I18nHelper.get(i18nKey));
    }

    public void showSuccessI18n(String i18nKey, Object... formatArgs) {
        showSuccess(String.format(
                I18nHelper.get(i18nKey), formatArgs
        ));
    }

    public void showSuccess(String successMessage) {
        createNotification()
                .ifPresent(n -> n.position(Pos.TOP_CENTER)
                        .text(successMessage)
                        .graphic(FontIcon.of(FontAwesome.CHECK_CIRCLE, 32, Color.GREEN))
                        .hideAfter(Duration.seconds(2))
                        .show()
                );
    }

    public void showErrorI18n(String i18nKey) {
        showError(I18nHelper.get(i18nKey));
    }

    public void showError(String errorMessage) {
        createNotification()
                .ifPresent(n -> n.position(Pos.TOP_CENTER)
                        .text(errorMessage)
                        .graphic(FontIcon.of(FontAwesome.EXCLAMATION_TRIANGLE, 32, Color.ORANGERED))
                        .hideAfter(Duration.seconds(2))
                        .show()
                );
    }

    public void showInfoI18n(String i18nKey) {
        showInfo(I18nHelper.get(i18nKey));
    }

    public void showInfoI18n(String i18nKey, Object... formatArgs) {
        showInfo(String.format(
                I18nHelper.get(i18nKey), formatArgs
        ));
    }

    public void showInfo(String infoMessage) {
        createNotification()
                .ifPresent(n -> n.position(Pos.TOP_CENTER)
                        .text(infoMessage)
                        .graphic(FontIcon.of(FontAwesome.INFO_CIRCLE, 32, Color.DODGERBLUE))
                        .hideAfter(Duration.seconds(2))
                        .show()
                );
    }

    private Optional<Notifications> createNotification() {
        return Window.getWindows().isEmpty() ? Optional.empty() : Optional.of(Notifications.create());
    }

    public void showException(Throwable e) {
        ExceptionDialog dialog = new ExceptionDialog(e);

        dialog.setTitle(I18nHelper.get(I18nKeys.ERROR));
        dialog.setContentText(I18nHelper.get(I18nKeys.ERROR_CONTEXT_MESSAGE));
        buildExceptionDialogBtn(dialog);
        dialog.show();
        log.error("Unknown exception", e);
    }

    private void buildExceptionDialogBtn(ExceptionDialog dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        ObservableList<Node> children = dialogPane.getChildren();
        ButtonBar buttonBar = (ButtonBar) children.stream()
                .filter(node -> node instanceof ButtonBar)
                .findFirst()
                .orElseThrow();
        ObservableList<Node> buttons = buttonBar.getButtons();
        Button reportBtn;

        dialogPane.setHeaderText(I18nHelper.get(I18nKeys.ERROR_HEADER_TITLE));
        buttons.forEach(node -> {
            if (node instanceof Button button) {
                String text = button.getText();

                if (text.contains("OK")) {
                    button.setText(I18nHelper.get(I18nKeys.ERROR_CONFIRM));
                }
            }
        });
        reportBtn = new Button(I18nHelper.get(I18nKeys.ERROR_REPORT));
        reportBtn.setGraphic(FontIcon.of(FontAwesome.GITHUB, 16));
        reportBtn.setOnAction(event ->
                HostServiceHelper.showDocument(BaseValues.REPOSITORY_NEW_ISSUE_URL)
        );
        buttons.add(reportBtn);
    }
}
