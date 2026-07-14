package io.knifer.freebox.component.factory;

import io.knifer.freebox.component.node.EmojiableLabel;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.model.domain.SourceBeanCountItem;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.IndexedCheckModel;

import java.util.function.Consumer;

/**
 * 搜索源筛选侧边栏的单元格工厂。
 * <p>
 * 每项显示：源名称（支持 emoji）、搜索结果计数、复选框。
 * 通过 {@code checkedProxy} 与 {@link CheckListView} 的勾选模型双向同步，
 * 避免调用 {@code refresh()} 导致界面闪动。
 * </p>
 *
 * @author Knifer
 */
@RequiredArgsConstructor
public class SourceBeanSearchingCheckListCellFactory
        implements Callback<ListView<SourceBeanCountItem>, ListCell<SourceBeanCountItem>> {

    private final CheckListView<SourceBeanCountItem> checkListView;
    private final Consumer<SourceBean> onItemSelect;

    @Getter
    private final BooleanProperty checkableProperty = new SimpleBooleanProperty(true);

    @Override
    public ListCell<SourceBeanCountItem> call(ListView<SourceBeanCountItem> param) {
        ListCell<SourceBeanCountItem> cell = new ListCell<>() {

            private final CheckBox checkBox = new CheckBox();
            private final EmojiableLabel emojiLabel = new EmojiableLabel();
            private final Label movieCountLabel = new Label();
            private final HBox graphicBox = new HBox(checkBox, emojiLabel, movieCountLabel);
            private final BooleanProperty checkedProxy = new SimpleBooleanProperty();
            private final ListChangeListener<SourceBeanCountItem> checkedItemsListener = change -> {
                SourceBeanCountItem item = getItem();

                if (item != null) {
                    checkedProxy.set(checkListView.getCheckModel().isChecked(item));
                }
            };
            private SourceBeanCountItem boundItem;

            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                emojiLabel.getStyleClass().add("fse-medium");
                movieCountLabel.getStyleClass().add("fse-medium");
                movieCountLabel.getStyleClass().add("bold");
                movieCountLabel.getStyleClass().add("dark-orange");
                graphicBox.setAlignment(Pos.CENTER_LEFT);
                graphicBox.setSpacing(3);
                checkBox.visibleProperty().bind(checkableProperty);
                checkBox.managedProperty().bind(checkableProperty);
                checkBox.setFocusTraversable(false);
                checkBox.selectedProperty().bindBidirectional(checkedProxy);
                checkedProxy.addListener((obs, oldValue, newValue) -> {
                    SourceBeanCountItem item = getItem();
                    IndexedCheckModel<SourceBeanCountItem> checkModel;

                    if (item == null) {

                        return;
                    }
                    checkModel = checkListView.getCheckModel();
                    if (newValue) {
                        if (!checkModel.isChecked(item)) {
                            checkModel.check(item);
                        }
                    } else {
                        if (checkModel.isChecked(item)) {
                            checkModel.clearCheck(item);
                        }
                    }
                });
                checkListView.getCheckModel().getCheckedItems().addListener(checkedItemsListener);
            }

            @Override
            public void updateItem(SourceBeanCountItem item, boolean empty) {
                String sourceName;

                super.updateItem(item, empty);
                if (item == null || empty) {
                    if (boundItem != null) {
                        movieCountLabel.textProperty().unbind();
                        movieCountLabel.setText(StringUtils.EMPTY);
                        boundItem = null;
                    }
                    setGraphic(null);

                    return;
                }
                sourceName = item.getSourceBean().getName();
                emojiLabel.setText(sourceName);
                if (item != boundItem) {
                    if (boundItem != null) {
                        movieCountLabel.textProperty().unbind();
                    }
                    boundItem = item;
                    movieCountLabel.textProperty().bind(
                            Bindings.when(item.movieCountProperty().greaterThan(0))
                                    .then(Bindings.concat(" (", item.movieCountProperty(), ")"))
                                    .otherwise(StringUtils.EMPTY)
                    );
                }
                checkedProxy.set(checkListView.getCheckModel().isChecked(item));
                if (getGraphic() != graphicBox) {
                    setGraphic(graphicBox);
                }
            }
        };

        cell.focusedProperty().addListener((ob, oldValue, newValue) -> {
            SourceBeanCountItem item = cell.getItem();

            if (item != null && newValue) {
                onItemSelect.accept(item.getSourceBean());
            }
        });
        cell.setOnMouseClicked(event -> {
            onItemSelect.accept(cell.getItem().getSourceBean());
            if (checkableProperty.get() && event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                checkListView.getCheckModel().toggleCheckState(cell.getIndex());
            }
        });

        return cell;
    }
}
