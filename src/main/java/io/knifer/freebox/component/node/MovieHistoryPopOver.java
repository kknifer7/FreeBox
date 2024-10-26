package io.knifer.freebox.component.node;

import io.knifer.freebox.component.factory.VodInfoGridCellFactory;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.common.VodInfo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.GridView;
import org.controlsfx.control.PopOver;

import java.util.List;
import java.util.function.Consumer;

/**
 * 影视历史记录弹出内容框
 *
 * @author Knifer
 */
public class MovieHistoryPopOver extends PopOver {

    private final GridView<VodInfo> vodInfoGridView;
    private final BooleanProperty loadingProperty = new SimpleBooleanProperty(false);

    public MovieHistoryPopOver(Consumer<VodInfo> onItemDelete) {
        super();
        setOnShowing(evt -> setDetached(true));
        setTitle(I18nHelper.get(I18nKeys.TV_HISTORY));
        vodInfoGridView = new GridView<>();
        vodInfoGridView.setCellFactory(new VodInfoGridCellFactory(onItemDelete));
        vodInfoGridView.setHorizontalCellSpacing(50);
        vodInfoGridView.setVerticalCellSpacing(75);
        vodInfoGridView.disableProperty().bind(loadingProperty);
        closeButtonEnabledProperty().bind(loadingProperty.not());
        BorderPane root = new BorderPane();
        root.setPrefSize(570, 400);
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

    public void clearVodInfoList() {
        vodInfoGridView.getItems().clear();
    }

    public void setOnVodInfoGridViewClicked(EventHandler<? super MouseEvent> eventHandler) {
        vodInfoGridView.setOnMouseClicked(eventHandler);
    }

    public BooleanProperty loadingPropertyProperty() {
        return loadingProperty;
    }
}
