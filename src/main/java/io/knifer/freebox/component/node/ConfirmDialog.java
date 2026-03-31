package io.knifer.freebox.component.node;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.WindowHelper;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.util.function.Consumer;

/**
 * 确认对话框
 *
 * @author Knifer
 */
public class ConfirmDialog extends AbstractNotificationDialog<Boolean> {

    private final ButtonType confirmButtonType;
    private final ButtonType cancelButtonType;
    private final Consumer<Boolean> onResult;

    /**
     * 创建确认对话框
     *
     * @param title 标题
     * @param content 内容文本
     * @param onResult 结果回调（true为确认，false为取消）
     */
    public ConfirmDialog(String title, String content, Consumer<Boolean> onResult) {
        this(title, content, I18nHelper.get(I18nKeys.COMMON_OK), I18nHelper.get(I18nKeys.COMMON_CANCEL), onResult);
    }

    /**
     * 创建确认对话框
     *
     * @param title 标题
     * @param content 内容文本
     * @param confirmButtonText 确认按钮文本
     * @param cancelButtonText 取消按钮文本
     * @param onResult 结果回调（true为确认，false为取消）
     */
    public ConfirmDialog(
            String title,
            String content,
            String confirmButtonText,
            String cancelButtonText,
            Consumer<Boolean> onResult
    ) {
        super();

        this.confirmButtonType = new ButtonType(confirmButtonText, ButtonBar.ButtonData.OK_DONE);
        this.cancelButtonType = new ButtonType(cancelButtonText, ButtonBar.ButtonData.CANCEL_CLOSE);
        this.onResult = onResult;

        initDialog(title, content);
    }

    private void initDialog(String title, String content) {
        DialogPane dialogPane;
        VBox contentBox;
        Label contentLabel;

        dialogPane = getDialogPane();
        contentBox = new VBox();
        contentLabel = new Label(content);

        // 设置对话框标题
        setTitle(title);
        setHeaderText(null);

        // 设置内容区域
        contentBox.getStyleClass().add("confirm-dialog-content");
        contentLabel.getStyleClass().add("confirm-dialog-message");
        contentLabel.setWrapText(true);
        contentBox.getChildren().add(contentLabel);
        dialogPane.setContent(contentBox);

        // 添加按钮
        dialogPane.getButtonTypes().addAll(cancelButtonType, confirmButtonType);

        // 设置字体
        WindowHelper.setFontFamily(dialogPane, ConfigHelper.getUsageFontFamily());

        // 设置结果转换器
        setResultConverter(buttonType -> {
            boolean result;

            result = buttonType == confirmButtonType;

            return result;
        });

        // 设置模态
        initModality(Modality.APPLICATION_MODAL);

        // 设置关闭请求处理
        setOnHiding(event -> {
            Boolean result;

            if (onResult != null) {
                result = getResult();
                onResult.accept(result != null ? result : false);
            }
        });
    }

    /**
     * 显示确认对话框并等待用户响应
     *
     * @param titleI18nKey 标题国际化键
     * @param contentI18nKey 内容国际化键
     * @param onResult 结果回调（true为确认，false为取消）
     */
    public static void showI18n(String titleI18nKey, String contentI18nKey, Consumer<Boolean> onResult) {
        ConfirmDialog dialog;

        dialog = new ConfirmDialog(
                I18nHelper.get(titleI18nKey),
                I18nHelper.get(contentI18nKey),
                onResult
        );
        dialog.showAndWait();
    }
}
