package io.knifer.freebox.component.node;

import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.WindowHelper;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

/**
 * 通知窗口
 *
 * @author Knifer
 */
public abstract class AbstractNotificationDialog<T> extends Dialog<T> {

    public AbstractNotificationDialog() {
        super();
        DialogPane dialogPane = getDialogPane();
        Node hiddenBtn;

        // 设置一个隐藏的操作按钮，否则基于Dialog的特性，它将无法通过hide或者close方法关闭
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        hiddenBtn = dialogPane.lookupButton(ButtonType.CLOSE);
        hiddenBtn.setVisible(false);
        hiddenBtn.setManaged(false);
        WindowHelper.setFontFamily(dialogPane, ConfigHelper.getUsageFontFamily());
    }
}
