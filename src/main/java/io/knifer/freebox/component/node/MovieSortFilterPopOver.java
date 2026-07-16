package io.knifer.freebox.component.node;

import io.knifer.freebox.component.factory.MovieSortFilterTreeCellFactory;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.common.tvbox.MovieSort;
import io.knifer.freebox.model.domain.MovieSortFilterTreeNode;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckTreeView;
import org.controlsfx.control.PopOver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 影视分类过滤条件树弹出框
 *
 * @author Knifer
 */
public class MovieSortFilterPopOver extends PopOver {

    private MovieSort.SortData sortData;

    private final CheckTreeView<MovieSortFilterTreeNode> treeView;
    private final CheckBoxTreeItem<MovieSortFilterTreeNode> treeRoot = new CheckBoxTreeItem<>();

    public MovieSortFilterPopOver(Consumer<MovieSort.SortData> onFilterAction) {
        super();
        Button okBtn = new Button(I18nHelper.get(I18nKeys.COMMON_OK));
        Button resetBtn = new Button(I18nHelper.get(I18nKeys.COMMON_RESET));
        HBox btnHBox;
        VBox root;

        setTitle(I18nHelper.get(I18nKeys.TV_CLASS_FILTER));
        treeView = new CheckTreeView<>();
        treeView.setFocusTraversable(false);
        treeView.setShowRoot(false);
        treeView.setRoot(treeRoot);
        treeRoot.setExpanded(true);
        treeView.setCellFactory(new MovieSortFilterTreeCellFactory());
        resetBtn.setFocusTraversable(false);
        resetBtn.setOnAction(evt -> treeView.getCheckModel().clearChecks());
        okBtn.setFocusTraversable(false);
        okBtn.setOnAction(evt -> {
            ObservableList<TreeItem<MovieSortFilterTreeNode>> checkedItems = treeView.getCheckModel().getCheckedItems();
            Map<String, String> checkedFilterKeyAndValueMap;
            Map<String, String> filterSelect;

            if (sortData == null) {
                this.hide();

                return;
            }
            if (checkedItems.isEmpty()) {
                checkedFilterKeyAndValueMap = Map.of();
            } else {
                checkedFilterKeyAndValueMap = new HashMap<>();
                checkedItems.forEach(item -> {
                    MovieSortFilterTreeNode filter = item.getValue();
                    TreeItem<MovieSortFilterTreeNode> parent;
                    MovieSortFilterTreeNode parentFilter;

                    if (!filter.getFilterValueFlag()) {
                        return;
                    }
                    parent = item.getParent();
                    if (parent == null) {
                        return;
                    }
                    parentFilter = parent.getValue();
                    if (parentFilter.getFilterValueFlag()) {
                        return;
                    }
                    checkedFilterKeyAndValueMap.put(parentFilter.getFilterKey(), filter.getFilterValue());
                });
            }
            filterSelect = sortData.getFilterSelect();
            if (!checkedFilterKeyAndValueMap.equals(filterSelect)) {
                filterSelect.clear();
                filterSelect.putAll(checkedFilterKeyAndValueMap);
                onFilterAction.accept(sortData);
            }
            this.hide();
        });
        btnHBox = new HBox(resetBtn, okBtn);
        btnHBox.setSpacing(3);
        btnHBox.setAlignment(Pos.BASELINE_RIGHT);
        root = new VBox(treeView, btnHBox);
        root.setPadding(new Insets(10, 10, 10, 10));
        root.setSpacing(5);
        setContentNode(root);
    }

    public void putSortDataFilterList(MovieSort.SortData sortData) {
        ObservableList<TreeItem<MovieSortFilterTreeNode>> items;
        MovieSortFilterCheckBoxTreeItem treeItem;
        String filterVal;

        this.sortData = sortData;
        items = treeRoot.getChildren();
        items.clear();
        treeView.getCheckModel().clearChecks();
        for (MovieSort.SortFilter filter : sortData.getFilters()) {
            treeItem = MovieSortFilterCheckBoxTreeItem.from(filter);
            items.add(treeItem);
            for (TreeItem<MovieSortFilterTreeNode> child : treeItem.getChildren()) {
                // 如果sortData中的filterSelect中包含了选中的filterValue，则选中该item
                filterVal = sortData.getFilterSelect().get(filter.getKey());
                if (filterVal != null && filterVal.equals(child.getValue().getFilterValue())) {
                    ((CheckBoxTreeItem<MovieSortFilterTreeNode>) child).setSelected(true);
                }
            }
        }
    }
}
