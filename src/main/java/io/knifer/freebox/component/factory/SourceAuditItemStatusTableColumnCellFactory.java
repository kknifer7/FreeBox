package io.knifer.freebox.component.factory;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.domain.SourceAuditItem;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * 播放源审计项表格列单元格工厂
 *
 * @author Knifer
 */
public class SourceAuditItemStatusTableColumnCellFactory implements
        Callback<TableColumn<SourceAuditItem, SourceAuditStatus>, TableCell<SourceAuditItem, SourceAuditStatus>> {

    @Override
    public TableCell<SourceAuditItem, SourceAuditStatus> call(TableColumn<SourceAuditItem, SourceAuditStatus> param) {
        return new TableCell<>() {

            @Override
            protected void updateItem(SourceAuditStatus item, boolean empty) {
                Label label;

                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);

                    return;
                }
                this.setAlignment(Pos.CENTER);
                label = new Label();
                switch (item) {
                    case INITIALIZED -> {
                        label.setGraphic(FontIcon.of(FontAwesome.PAUSE_CIRCLE, 15, Color.GRAY));
                        label.setText(I18nHelper.get(I18nKeys.SOURCE_AUDIT_AUDIT_STATUS_INITIALIZED));
                    }
                    case SKIPPED -> {
                        label.setGraphic(FontIcon.of(FontAwesome.LEAF, 15, Color.ORANGE));
                        label.setText(I18nHelper.get(I18nKeys.SOURCE_AUDIT_AUDIT_STATUS_SKIPPED));
                    }
                    case PROCESSING -> {
                        label.setGraphic(FontIcon.of(FontAwesome.HOURGLASS_HALF, 15, Color.DODGERBLUE));
                        label.setText(I18nHelper.get(I18nKeys.SOURCE_AUDIT_AUDIT_STATUS_PROCESSING));
                    }
                    case SUCCESS -> {
                        label.setGraphic(FontIcon.of(FontAwesome.CHECK_CIRCLE, 15, Color.GREEN));
                        label.setText(I18nHelper.get(I18nKeys.SOURCE_AUDIT_AUDIT_STATUS_SUCCESS));
                    }
                    case FAILED -> {
                        label.setGraphic(FontIcon.of(FontAwesome.REMOVE, 15, Color.RED));
                        label.setText(I18nHelper.get(I18nKeys.SOURCE_AUDIT_AUDIT_STATUS_FAILED));
                    }
                }
                setGraphic(label);
            }
        };
    }
}
