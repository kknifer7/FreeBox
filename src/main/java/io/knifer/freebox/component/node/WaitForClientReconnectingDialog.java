package io.knifer.freebox.component.node;

import io.knifer.freebox.component.router.Router;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.net.websocket.core.ClientManager;
import jakarta.inject.Inject;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.util.List;

/**
 * 等待客户端重连窗口
 *
 * @author Knifer
 */
public class WaitForClientReconnectingDialog extends AbstractNotificationDialog<Void> {

    @Inject
    public WaitForClientReconnectingDialog(Router router, ClientManager clientManager) {
        super();
        ProgressIndicator pi = new ProgressIndicator();
        Button cancelBtn = new Button(I18nHelper.get(I18nKeys.COMMON_CANCEL_AND_BACK_TO_HOME));
        DialogPane dialogPane = getDialogPane();
        VBox root;

        cancelBtn.setOnAction(evt -> {
            Stage stage = router.getCurrent();
            List<Stage> stages = router.popAll();

            if (stage != null) {
                closeStage(stage);
            }
            for (Stage s : stages) {
                if (s == router.getPrimary()) {
                    if (!s.isShowing()) {
                        s.show();
                    }
                    break;
                }
                closeStage(s);
            }
            router.resetCurrent();
            if (isShowing()) {
                close();
            }
            clientManager.shutdownConnectingExecutor();
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

    private void closeStage(Stage stage) {
        if (stage.isShowing()) {
            stage.hide();
            Event.fireEvent(stage, new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }
}
