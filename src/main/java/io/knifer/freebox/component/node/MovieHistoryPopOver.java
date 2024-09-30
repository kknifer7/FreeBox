package io.knifer.freebox.component.node;

import io.knifer.freebox.component.factory.VodInfoGridCellFactory;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.common.VodInfo;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.GridView;
import org.controlsfx.control.PopOver;

import java.util.List;

/**
 * 影视历史记录弹出内容框
 *
 * @author Knifer
 */
public class MovieHistoryPopOver extends PopOver {

    private final GridView<VodInfo> vodInfoGridView;
    private final BorderPane root;

    public MovieHistoryPopOver() {
        // TODO 调整布局、开发功能逻辑
        super();
        setOnShowing(evt -> setDetached(true));
        setTitle(I18nHelper.get(I18nKeys.TV_HISTORY));
        vodInfoGridView = new GridView<>();
        vodInfoGridView.setCellFactory(new VodInfoGridCellFactory());
        vodInfoGridView.setHorizontalCellSpacing(50);
        vodInfoGridView.setVerticalCellSpacing(75);
        root = new BorderPane();
        root.setPrefSize(500, 400);
        root.setCenter(vodInfoGridView);
        setContentNode(root);
    }

    public void setVodInfoList(List<VodInfo> vodInfoList) {
        ObservableList<VodInfo> items = vodInfoGridView.getItems();

        if (!items.isEmpty()) {
            items.clear();
        }
        items.addAll(vodInfoList);
    }
}
