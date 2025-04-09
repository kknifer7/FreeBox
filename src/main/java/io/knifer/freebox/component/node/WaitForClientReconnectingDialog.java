package io.knifer.freebox.component.node;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.I18nHelper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 等待客户端重连窗口
 *
 * @author Knifer
 */
public class WaitForClientReconnectingDialog extends Dialog<Void> {

    public WaitForClientReconnectingDialog() {
        super();
        ProgressIndicator pi = new ProgressIndicator();
        Button cancelBtn = new Button(I18nHelper.get(I18nKeys.COMMON_CANCEL_AND_BACK_TO_HOME));
        DialogPane dialogPane = getDialogPane();
        Node hiddenBtn;
        VBox root;

        cancelBtn.setOnAction(evt -> {
            Stage stage = Context.INSTANCE.getCurrentStage();

            if (stage != null) {
                stage.hide();
            }
            while ((stage = Context.INSTANCE.popStage()) != null) {
                if (stage == Context.INSTANCE.getPrimaryStage()) {
                    if (!stage.isShowing()) {
                        stage.show();
                    }
                    break;
                }
                if (stage.isShowing()) {
                    stage.hide();
                }
            }
            hide();
            Context.INSTANCE.getClientManager().shutdownConnectingExecutor();
        });
        cancelBtn.setFocusTraversable(false);
        pi.setPrefSize(30, 30);
        root = new VBox(new StackPane(pi), cancelBtn);
        root.setAlignment(Pos.CENTER);
        root.setSpacing(10);
        dialogPane.contentProperty().set(root);
        setHeaderText(I18nHelper.get(I18nKeys.ERROR_RECONNECTING_CLIENT));
        initStyle(StageStyle.UNDECORATED);
        // 要设置一个隐藏的操作按钮，否则基于Dialog的特性，它将无法通过hide或者close方法关闭
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        hiddenBtn = dialogPane.lookupButton(ButtonType.CLOSE);
        hiddenBtn.setVisible(false);
        hiddenBtn.setManaged(false);
    }
}
