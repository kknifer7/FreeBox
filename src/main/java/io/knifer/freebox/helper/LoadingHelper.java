package io.knifer.freebox.helper;

import io.knifer.freebox.component.node.LoadingDialog;
import io.knifer.freebox.component.node.WaitForClientReconnectingDialog;
import io.knifer.freebox.constant.I18nKeys;
import javafx.stage.Stage;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 加载
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class LoadingHelper {

    private static LoadingDialog loadingDialog = null;
    private static WaitForClientReconnectingDialog waitingReconnectingDialog = null;

    public void showLoading(Stage ownerStage) {
        showLoading(ownerStage, I18nKeys.MESSAGE_LOADING);
    }

    public void showLoading(Stage ownerStage, String headerTextI18nKey) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            return;
        }
        loadingDialog = new LoadingDialog();
        loadingDialog.initOwner(ownerStage);
        loadingDialog.setHeaderText(I18nHelper.get(headerTextI18nKey));
        loadingDialog.show();
    }

    public void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.hide();
        }
    }

    public void showWaitingReconnecting(Stage ownerStage) {
        if (waitingReconnectingDialog != null && waitingReconnectingDialog.isShowing()) {
            return;
        }
        waitingReconnectingDialog = new WaitForClientReconnectingDialog();
        waitingReconnectingDialog.initOwner(ownerStage);
        waitingReconnectingDialog.show();
    }

    public void hideWaitingReconnecting() {
        if (waitingReconnectingDialog != null && waitingReconnectingDialog.isShowing()) {
            waitingReconnectingDialog.hide();
        }
    }
}
