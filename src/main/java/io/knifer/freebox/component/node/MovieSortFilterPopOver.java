package io.knifer.freebox.component.node;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.common.tvbox.MovieSort;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Data;
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

    private final CheckTreeView<MovieSortFilterTreeItem> treeView;
    private final CheckBoxTreeItem<MovieSortFilterTreeItem> treeRoot = new CheckBoxTreeItem<>();

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
        treeRoot.setExpanded( true);
        treeView.setCellFactory(new Callback<>() {
            @Override
            public TreeCell<MovieSortFilterTreeItem> call(TreeView<MovieSortFilterTreeItem> view) {
                CheckBoxTreeCell<MovieSortFilterTreeItem> cell = new CheckBoxTreeCell<>() {
                    @Override
                    public void updateItem(MovieSortFilterTreeItem item, boolean empty) {
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

                cell.setOnMouseClicked(evt -> {
                    if (evt.getClickCount() == 2 && evt.getButton() == MouseButton.PRIMARY) {
                        treeView.getCheckModel().toggleCheckState(cell.getTreeItem());
                    }
                });

                return cell;
            }
        });
        resetBtn.setFocusTraversable(false);
        resetBtn.setOnAction(evt -> treeView.getCheckModel().clearChecks());
        okBtn.setFocusTraversable(false);
        okBtn.setOnAction(evt -> {
            ObservableList<TreeItem<MovieSortFilterTreeItem>> checkedItems = treeView.getCheckModel().getCheckedItems();
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
                    MovieSortFilterTreeItem filter = item.getValue();
                    TreeItem<MovieSortFilterTreeItem> parent;
                    MovieSortFilterTreeItem parentFilter;

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
        ObservableList<TreeItem<MovieSortFilterTreeItem>> items;

        this.sortData = sortData;
        items = treeRoot.getChildren();
        items.clear();
        treeView.getCheckModel().clearChecks();
        sortData.getFilters().forEach(filter -> {
            CheckBoxTreeItem<MovieSortFilterTreeItem> treeItem = new CheckBoxTreeItem<>(convertToTreeItem(filter));
            ObservableList<TreeItem<MovieSortFilterTreeItem>> itemChildren = treeItem.getChildren();

            treeItem.setIndependent(true);
            treeItem.setExpanded(true);
            items.add(treeItem);
            filter.getValues().entrySet().forEach(filterValueNameAndValue -> {
                CheckBoxTreeItem<MovieSortFilterTreeItem> childItem = new CheckBoxTreeItem<>(convertToTreeItem(filterValueNameAndValue));
                String filterVal;

                itemChildren.add(childItem);
                childItem.setIndependent(true);
                // 如果sortData中的filterSelect中包含了选中的filterValue，则选中该item
                filterVal = sortData.getFilterSelect().get(filter.getKey());
                if (filterVal != null && filterVal.equals(filterValueNameAndValue.getValue())) {
                    childItem.setSelected(true);
                }
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
        });

    }

    private MovieSortFilterTreeItem convertToTreeItem(MovieSort.SortFilter sortDataFilter) {
        MovieSortFilterTreeItem item = new MovieSortFilterTreeItem();

        item.setFilterValueFlag(false);
        item.setFilterKey(sortDataFilter.getKey());
        item.setFilterName(sortDataFilter.getName());

        return item;
    }

    private MovieSortFilterTreeItem convertToTreeItem(Map.Entry<String, String> filterValueNameAndValue) {
        MovieSortFilterTreeItem item = new MovieSortFilterTreeItem();

        item.setFilterValueFlag(true);
        item.setFilterValueName(filterValueNameAndValue.getKey());
        item.setFilterValue(filterValueNameAndValue.getValue());

        return item;
    }

    @Data
    private static class MovieSortFilterTreeItem {
        private String filterKey;
        private String filterName;
        private String filterValueName;
        private String filterValue;
        /**
         * 是否为filter里面的value（如果不是，就是外层的filter）
         */
        private Boolean filterValueFlag;
    }
}
