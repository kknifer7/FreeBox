package io.knifer.freebox.component.node;

import io.knifer.freebox.model.common.tvbox.MovieSort;
import io.knifer.freebox.model.domain.MovieSortFilterTreeNode;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;

/**
 * 影视筛选条件 树状视图选择框选项
 *
 * @author Knifer
 */
public class MovieSortFilterCheckBoxTreeItem extends CheckBoxTreeItem<MovieSortFilterTreeNode> {

    private MovieSortFilterCheckBoxTreeItem(MovieSortFilterTreeNode item) {
        super(item);
    }

    public static MovieSortFilterCheckBoxTreeItem from(MovieSort.SortFilter sortDataFilter) {
        MovieSortFilterCheckBoxTreeItem result = new MovieSortFilterCheckBoxTreeItem(
                MovieSortFilterTreeNode.from(sortDataFilter)
        );
        ObservableList<TreeItem<MovieSortFilterTreeNode>> itemChildren = result.getChildren();

        result.setIndependent(true);
        result.setExpanded(true);
        sortDataFilter.getValues().entrySet().forEach(filterValueNameAndValue -> {
            CheckBoxTreeItem<MovieSortFilterTreeNode> childItem = new CheckBoxTreeItem<>(
                    MovieSortFilterTreeNode.from(sortDataFilter.getKey(), filterValueNameAndValue)
            );

            itemChildren.add(childItem);
            childItem.setIndependent(true);
            childItem.selectedProperty().addListener((ob, oldVal, newVal) -> {
                if (!newVal) {
                    return;
                }
                itemChildren.forEach(i -> {
                    CheckBoxTreeItem<?> iCheckItem;

                    if (i.equals(childItem)) {
                        return;
                    }
                    iCheckItem = (CheckBoxTreeItem<?>) i;
                    if (iCheckItem.isSelected()) {
                        iCheckItem.setSelected(false);
                    }
                });
            });
        });

        return result;
    }
}
