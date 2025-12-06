package io.knifer.freebox.helper;

import cn.hutool.core.collection.CollUtil;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.ButtonTypes;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.util.CastUtil;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.Notifications;
import org.controlsfx.dialog.ExceptionDialog;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 提示
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class ToastHelper {

    private final AtomicBoolean showError = new AtomicBoolean(true);

    public void showSuccessI18n(String i18nKey) {
        showSuccess(I18nHelper.get(i18nKey));
    }

    public void showSuccessI18n(String i18nKey, Object... formatArgs) {
        showSuccess(String.format(
                I18nHelper.get(i18nKey), formatArgs
        ));
    }

    public void showSuccess(String successMessage) {
        createNotification(successMessage)
                .ifPresent(n -> {
                    n.position(Pos.TOP_CENTER)
                            .graphic(FontIcon.of(FontAwesome.CHECK_CIRCLE, 32, Color.GREEN))
                            .show();
                    postProcessNotification(Pos.TOP_CENTER);
                });
    }

    public void showErrorI18n(String i18nKey) {
        showError(I18nHelper.get(i18nKey));
    }

    public void showError(String errorMessage) {
        if (!showError.get()) {
            return;
        }
        createNotification(errorMessage)
                .ifPresent(n -> {
                    n.position(Pos.TOP_CENTER)
                            .graphic(FontIcon.of(FontAwesome.EXCLAMATION_TRIANGLE, 32, Color.ORANGERED))
                            .show();
                    postProcessNotification(Pos.TOP_CENTER);
                });
    }

    public void showWarningI18n(String i18nKey) {
        showWarning(I18nHelper.get(i18nKey));
    }

    public void showWarning(String warningMessage) {
        createNotification(warningMessage)
                .ifPresent(n -> {
                    n.position(Pos.TOP_CENTER)
                            .graphic(FontIcon.of(FontAwesome.EXCLAMATION_TRIANGLE, 32, Color.ORANGE))
                            .show();
                    postProcessNotification(Pos.TOP_CENTER);
                });
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
        createNotification(infoMessage)
                .ifPresent(n -> {
                    n.position(Pos.TOP_CENTER)
                            .graphic(FontIcon.of(FontAwesome.INFO_CIRCLE, 32, Color.DODGERBLUE))
                            .show();
                    postProcessNotification(Pos.TOP_CENTER);
                });
    }

    private Optional<Notifications> createNotification(String message) {
        if (Window.getWindows().isEmpty()) {

            return Optional.empty();
        }

        return Window.getWindows().isEmpty() ?
                Optional.empty() :
                Optional.of(
                        Notifications.create()
                                .text(message)
                                .hideAfter(Duration.seconds(2))
                );
    }

    /**
     * 对Notification的样式进行后置处理（需要先把Notification show出来）
     * @param pos Notification的位置，用于查询对应的Notification弹窗
     */
    private void postProcessNotification(Pos pos) {
        Class<?> clazz;
        Field field;
        Object instanceObj;
        Map<Pos, List<Popup>> popupMaps;
        Popup popup;

        try {
            // 通过反射对Notification的样式（字体等）进行后置处理
            clazz = Class.forName("org.controlsfx.control.Notifications$NotificationPopupHandler");
            field = clazz.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            instanceObj = field.get(null);
            field = clazz.getDeclaredField("popupsMap");
            field.setAccessible(true);
            popupMaps = CastUtil.cast(field.get(instanceObj));
            if (popupMaps != null && !popupMaps.isEmpty()) {
                // 获取到最新的Notification弹窗，进行样式修改
                popup = CollUtil.getLast(popupMaps.get(pos));
                WindowHelper.setFontFamily(popup, ConfigHelper.getUsageFontFamily());
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            log.error("Reflection error", e);
        }
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
        WindowHelper.setFontFamily(dialog.getDialogPane(), ConfigHelper.getUsageFontFamily());
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

    public void showErrorAlert(
            String headerI18n,
            String contentI18n,
            @Nullable  EventHandler<DialogEvent> onCloseRequest
    ) {
        Alert alert = new Alert(Alert.AlertType.ERROR, I18nHelper.get(contentI18n), ButtonTypes.OK);

        alert.setTitle(I18nHelper.get(I18nKeys.ERROR));
        alert.setHeaderText(I18nHelper.get(headerI18n));
        if (onCloseRequest != null) {
            alert.setOnCloseRequest(onCloseRequest);
        }
        WindowHelper.setFontFamily(alert.getDialogPane(), ConfigHelper.getUsageFontFamily());
        alert.show();
    }

    public void enableErrorShow() {
        showError.set(true);
    }

    public void disableErrorShow() {
        showError.set(false);
    }
}
