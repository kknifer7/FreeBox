package io.knifer.freebox.component.factory;

import io.knifer.freebox.model.common.tvbox.SourceBean;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * 源信息单元格工厂
 *
 * @author Knifer
 */
@RequiredArgsConstructor
public class SourceBeanProblemListCellFactory implements Callback<ListView<SourceBean>, ListCell<SourceBean>> {

    private final Consumer<SourceBean> onItemSelect;

    @Override
    public ListCell<SourceBean> call(ListView<SourceBean> param) {
        ListCell<SourceBean> cell = new ListCell<>() {
            @Override
            public void updateItem(SourceBean item, boolean empty) {
                if (Platform.isFxApplicationThread()) {
                    update(item, empty);
                } else {
                    Platform.runLater(() -> update(item, empty));
                }
            }

            private void update(SourceBean item, boolean empty) {
                Label label;

                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(StringUtils.EMPTY);
                    setGraphic(null);

                    return;
                }
                label = new Label(item.getName());
                label.setGraphic(FontIcon.of(FontAwesome.TIMES_CIRCLE, 20, Color.RED));
                setGraphic(label);
            }
        };

        cell.focusedProperty().addListener((ob, oldValue, newValue) -> {
            if (newValue) {
                onItemSelect.accept(cell.getItem());
            }
        });
        cell.setOnMouseClicked(evt -> onItemSelect.accept(cell.getItem()));

        return cell;
    }
}
