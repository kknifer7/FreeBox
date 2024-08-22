package io.knifer.freebox.component.factory;

import io.knifer.freebox.model.common.MovieSort;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.util.Callback;

/**
 * 网站影视类别列表单元格工厂
 *
 * @author Knifer
 */
public class ClassListCellFactory implements Callback<ListView<MovieSort.SortData>, ListCell<MovieSort.SortData>> {
    @Override
    public ListCell<MovieSort.SortData> call(ListView<MovieSort.SortData> param) {
        return new ListCell<>() {
            @Override
            protected void updateItem(MovieSort.SortData item, boolean empty) {
                Label nameLabel;
                HBox containerHBox;

                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);

                    return;
                }
                nameLabel = new Label(item.getName());
                nameLabel.setFont(Font.font(nameLabel.getFont().getFamily(), 16));
                containerHBox = new HBox();
                containerHBox.setAlignment(Pos.CENTER);
                containerHBox.getChildren().add(nameLabel);
                setGraphic(containerHBox);
            }
        };
    }
}
