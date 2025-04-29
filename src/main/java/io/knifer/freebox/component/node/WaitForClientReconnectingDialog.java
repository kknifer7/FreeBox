package io.knifer.freebox.component.node;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.I18nHelper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 等待客户端重连窗口
 *
 * @author Knifer
 */
public class WaitForClientReconnectingDialog extends AbstractNotificationDialog<Void> {

    public WaitForClientReconnectingDialog() {
        super();
        ProgressIndicator pi = new ProgressIndicator();
        Button cancelBtn = new Button(I18nHelper.get(I18nKeys.COMMON_CANCEL_AND_BACK_TO_HOME));
        DialogPane dialogPane = getDialogPane();
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
    }
}
