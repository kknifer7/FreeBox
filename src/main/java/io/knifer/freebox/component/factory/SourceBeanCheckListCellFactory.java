package io.knifer.freebox.component.factory;

import io.knifer.freebox.component.node.EmojiableLabel;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.CheckListView;

import java.util.List;
import java.util.function.Consumer;

/**
 * 源信息单元格工厂
 *
 * @author Knifer
 */
@RequiredArgsConstructor
public class SourceBeanCheckListCellFactory implements Callback<ListView<SourceBean>, ListCell<SourceBean>> {

    private final CheckListView<SourceBean> checkListView;
    private final Consumer<SourceBean> onItemSelect;

    @Override
    public ListCell<SourceBean> call(ListView<SourceBean> param) {
        CheckBoxListCell<SourceBean> cell = new CheckBoxListCell<>(checkListView::getItemBooleanProperty) {

            private final EmojiableLabel emojiLabel;

            {
                List<String> styleClasses;

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                emojiLabel = new EmojiableLabel();
                styleClasses = emojiLabel.getStyleClass();
                styleClasses.add("fse-medium");
            }

            @Override
            public void updateItem(SourceBean item, boolean empty) {
                Node graphic;
                HBox newGraphic;

                super.updateItem(item, empty);
                if (item == null || empty) {
                    setGraphic(null);

                    return;
                }
                emojiLabel.setText(item.getName());
                graphic = getGraphic();
                if (graphic instanceof CheckBox) {
                    // 初始化Graphic
                    newGraphic = new HBox(graphic, emojiLabel);
                    newGraphic.setAlignment(Pos.CENTER_LEFT);
                    newGraphic.setSpacing(3);
                    setGraphic(newGraphic);
                }
            }
        };

        cell.focusedProperty().addListener((ob, oldValue, newValue) -> {
            if (newValue) {
                onItemSelect.accept(cell.getItem());
            }
        });
        cell.setOnMouseClicked(event -> {
            onItemSelect.accept(cell.getItem());
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                checkListView.getCheckModel().toggleCheckState(cell.getIndex());
            }
        });

        return cell;
    }
}
