package io.knifer.freebox.component.factory;

import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.model.domain.SpiderDebugging;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;

/**
 * 调试爬虫列表单元格
 *
 * @author Knifer
 */
public class SpiderDebuggingListCell extends ListCell<SpiderDebugging> {

    @Override
    protected void updateItem(SpiderDebugging spiderDebugging, boolean empty) {
        ImageView logoImageView;

        super.updateItem(spiderDebugging, empty);
        if (spiderDebugging == null || empty) {
            setText(null);
            setGraphic(null);

            return;
        }
        logoImageView = new ImageView(BaseResources.JAVASCRIPT_LOGO_IMG);
        logoImageView.setFitWidth(16);
        logoImageView.setFitHeight(16);
        setText(spiderDebugging.getSourceFilePath());
        setGraphic(logoImageView);
    }
}
