package io.knifer.freebox.component.factory;

import io.knifer.freebox.model.common.SourceBean;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseButton;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckListView;

import java.util.function.Consumer;

/**
 * 源信息单元格工厂
 *
 * @author Knifer
 */
@RequiredArgsConstructor
public class SourceBeanListCellFactory implements Callback<ListView<SourceBean>, ListCell<SourceBean>> {

    private final CheckListView<SourceBean> checkListView;
    private final Consumer<SourceBean> onItemFocus;

    @Override
    public ListCell<SourceBean> call(ListView<SourceBean> param) {
        CheckBoxListCell<SourceBean> cell = new CheckBoxListCell<>(checkListView::getItemBooleanProperty) {
            @Override
            public void updateItem(SourceBean item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(StringUtils.EMPTY);

                    return;
                }
                setText(item.getName());
            }
        };

        cell.focusedProperty().addListener((ob, oldValue, newValue) -> {
            if (newValue) {
                onItemFocus.accept(cell.getItem());
            }
        });
        cell.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                checkListView.getCheckModel().toggleCheckState(cell.getIndex());
            }
        });

        return cell;
    }
}
