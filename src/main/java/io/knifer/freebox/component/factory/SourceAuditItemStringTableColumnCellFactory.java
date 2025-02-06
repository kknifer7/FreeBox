package io.knifer.freebox.component.factory;

import io.knifer.freebox.model.domain.SourceAuditItem;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * 播放源审计项表格列单元格工厂
 *
 * @author Knifer
 */
public class SourceAuditItemStringTableColumnCellFactory implements
        Callback<TableColumn<SourceAuditItem, String>, TableCell<SourceAuditItem, String>> {

    @Override
    public TableCell<SourceAuditItem, String> call(TableColumn<SourceAuditItem, String> param) {
        return new TableCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);

                    return;
                }
                this.setAlignment(Pos.CENTER);
                setText(item);
            }
        };
    }
}
