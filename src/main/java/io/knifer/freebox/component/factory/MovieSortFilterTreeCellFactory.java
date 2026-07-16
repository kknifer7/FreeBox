package io.knifer.freebox.component.factory;

import io.knifer.freebox.model.domain.MovieSortFilterTreeNode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.input.MouseButton;
import javafx.util.Callback;
import org.controlsfx.control.CheckTreeView;

/**
 * 影视浏览筛选树状数据单元格工程
 *
 * @author Knifer
 */
public class MovieSortFilterTreeCellFactory
        implements Callback<TreeView<MovieSortFilterTreeNode>, TreeCell<MovieSortFilterTreeNode>> {

    @Override
    public TreeCell<MovieSortFilterTreeNode> call(TreeView<MovieSortFilterTreeNode> treeView) {
        CheckBoxTreeCell<MovieSortFilterTreeNode> cell = new CheckBoxTreeCell<>() {
            @Override
            public void updateItem(MovieSortFilterTreeNode item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    return;
                }
                if (item.getFilterValueFlag()) {
                    setText(item.getFilterValueName());
                } else {
                    setText(item.getFilterName());
                    setGraphic(null);
                }
            }
        };

        if (treeView instanceof CheckTreeView<MovieSortFilterTreeNode> checkTreeView) {
            cell.setOnMouseClicked(evt -> {
                if (evt.getClickCount() == 2 && evt.getButton() == MouseButton.PRIMARY) {
                    checkTreeView.getCheckModel().toggleCheckState(cell.getTreeItem());
                }
            });
        }

        return cell;
    }
}
