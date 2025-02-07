package io.knifer.freebox.helper;

import io.knifer.freebox.component.node.LoadingDialog;
import javafx.stage.Stage;
import lombok.experimental.UtilityClass;

/**
 * 加载
 *
 * @author Knifer
 */
@UtilityClass
public class LoadingHelper {

    private static LoadingDialog loadingDialog = null;

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
            loadingDialog.close();
        }
    }
}
