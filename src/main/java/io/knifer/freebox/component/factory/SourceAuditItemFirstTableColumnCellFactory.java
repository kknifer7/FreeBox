package io.knifer.freebox.component.factory;

import io.knifer.freebox.model.domain.SourceAuditItem;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

/**
 * 播放源审计项表格首列单元格工厂
 *
 * @author Knifer
 */
public class SourceAuditItemFirstTableColumnCellFactory implements
        Callback<TableColumn<SourceAuditItem, String>, TableCell<SourceAuditItem, String>> {

    private final static Font FONT = Font.font(
            Font.getDefault().getFamily(),
            FontWeight.BOLD,
            Font.getDefault().getSize()
    );

    @Override
    public TableCell<SourceAuditItem, String> call(TableColumn<SourceAuditItem, String> param) {
        return new TableCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                Label label;

                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);

                    return;
                }
                label = new Label(item);
                label.setFont(FONT);
                this.setAlignment(Pos.CENTER);
                setGraphic(label);
            }
        };
    }
}
