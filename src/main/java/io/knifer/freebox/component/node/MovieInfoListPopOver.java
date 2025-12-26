package io.knifer.freebox.component.node;

import io.knifer.freebox.component.factory.VodInfoGridCellFactory;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.model.common.tvbox.VodInfo;
import io.knifer.freebox.util.CollectionUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.GridView;
import org.controlsfx.control.PopOver;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * 影视数据列表弹出框
 *
 * @author Knifer
 */
public class MovieInfoListPopOver extends PopOver {

    private final GridView<VodInfo> vodInfoGridView;
    private final VodInfoGridCellFactory vodInfoGridViewCellFactory;
    private final BooleanProperty loadingProperty = new SimpleBooleanProperty(false);

    public MovieInfoListPopOver(
            String titleI18n,
            Consumer<VodInfo> onItemAction,
            Consumer<VodInfo> onItemDelete
    ) {
        super();
        setOnShowing(evt -> setDetached(true));
        setTitle(I18nHelper.get(titleI18n));
        vodInfoGridView = new GridView<>();
        vodInfoGridViewCellFactory = new VodInfoGridCellFactory(
                I18nKeys.TV_HISTORY.equals(titleI18n), onItemAction, onItemDelete
        );
        vodInfoGridView.setCellFactory(vodInfoGridViewCellFactory);
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

    public void setSourceBeans(Collection<SourceBean> sourceBeans) {
        if (CollectionUtil.isEmpty(sourceBeans)) {
            return;
        }
        ((VodInfoGridCellFactory) vodInfoGridView.getCellFactory()).setSourceBeans(sourceBeans);
    }

    public void clearVodInfoList() {
        vodInfoGridView.getItems().clear();
    }

    public BooleanProperty loadingPropertyProperty() {
        return loadingProperty;
    }

    public void destroy() {
        vodInfoGridViewCellFactory.destroy();
    }
}
