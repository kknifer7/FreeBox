package io.knifer.freebox.component.factory;

import io.knifer.freebox.model.domain.SourceAuditItem;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 播放源审计项表格列单元格工厂
 *
 * @author Knifer
 */
public class SourceAuditItemStartAtTableColumnCellFactory implements
        Callback<TableColumn<SourceAuditItem, LocalDateTime>, TableCell<SourceAuditItem, LocalDateTime>> {

    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public TableCell<SourceAuditItem, LocalDateTime> call(TableColumn<SourceAuditItem, LocalDateTime> param) {
        return new TableCell<>() {

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);

                    return;
                }
                this.setAlignment(Pos.CENTER);
                setText(item.format(FORMATTER));
            }
        };
    }
}
