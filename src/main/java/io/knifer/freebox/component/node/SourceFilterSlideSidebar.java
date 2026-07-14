package io.knifer.freebox.component.node;

import io.knifer.freebox.component.factory.SourceBeanSearchingCheckListCellFactory;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.model.domain.SourceBeanCountItem;
import io.knifer.freebox.util.NodeUtil;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.CheckModel;
import org.controlsfx.control.IndexedCheckModel;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 搜索结果源站点筛选侧边栏
 *
 * @author Knifer
 */
@Slf4j
public class SourceFilterSlideSidebar extends StackPane {

    private final VBox sidebar;
    private final CheckListView<SourceBeanCountItem> checkListView;
    private final TranslateTransition slideIn;
    private final TranslateTransition slideOut;
    /**
     * 侧边栏是否正在显示
     */
    @Getter
    private boolean sidebarShowing = false;

    private static final double SIDEBAR_WIDTH = 240.0;
    private static final Duration ANIM_DURATION = Duration.millis(250);
    private static final SourceBeanCountItem SELECT_ALL_ITEM;
    public static final String SELECT_ALL_SOURCE_KEY = "SELECT_ALL@FREE_BOX";
    private static final String SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP = "skip_checked_items_listener";

    static {
        SourceBean selectAllSourceBean = new SourceBean();

        selectAllSourceBean.setKey(SELECT_ALL_SOURCE_KEY);
        selectAllSourceBean.setName(I18nHelper.get(I18nKeys.COMMON_ALL));
        SELECT_ALL_ITEM = new SourceBeanCountItem(selectAllSourceBean, 0);
    }

    public SourceFilterSlideSidebar(
            BooleanProperty searchLoadingProperty,
            Consumer<Set<String>> onFilter,
            Consumer<SourceBean> onSelect
    ) {
        super();
        Button okBtn = new Button(I18nHelper.get(I18nKeys.COMMON_OK));
        Button resetBtn = new Button(I18nHelper.get(I18nKeys.COMMON_RESET));
        Button closeBtn = new Button(I18nHelper.get(I18nKeys.COMMON_CLOSE));
        HBox btnHBox;
        VBox sidebarContent;
        Label tipLabel = new Label();
        List<String> tipLabelStyleClasses;
        CheckModel<SourceBeanCountItem> checkModel;
        SourceBeanSearchingCheckListCellFactory cellFactory;
        ObservableMap<Object, Object> checkListViewProps;
        BooleanProperty checkableProperty;
        ObservableList<SourceBeanCountItem> checkedItems;

        // 侧边栏主体
        checkListView = new CheckListView<>();
        checkListView.setFocusTraversable(false);
        VBox.setVgrow(checkListView, Priority.ALWAYS);
        cellFactory = new SourceBeanSearchingCheckListCellFactory(checkListView, onSelect);
        checkModel = checkListView.getCheckModel();
        checkListViewProps = checkListView.getProperties();
        checkableProperty = cellFactory.getCheckableProperty();
        checkableProperty.addListener((ob, oldVal, newVal) -> {
            if (newVal) {
                NodeUtil.replaceStyleClass(tipLabel, "dark-orange", "success");
                // 变得可勾选后，重置所有勾选项，避免状态错乱
                checkListViewProps.put(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP, true);
                checkModel.clearChecks();
                checkListViewProps.put(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP, true);
                checkModel.check(SELECT_ALL_ITEM);
            } else {
                NodeUtil.replaceStyleClass(tipLabel, "success", "dark-orange");
            }
        });
        checkableProperty.bind(searchLoadingProperty.not());
        checkListView.setCellFactory(cellFactory);
        checkedItems = checkModel.getCheckedItems();
        checkedItems.addListener((ListChangeListener<SourceBeanCountItem>) change -> {
            List<? extends SourceBeanCountItem> addedSubList;
            boolean checkAllAdded = false;
            boolean otherAdded = false;

            if (checkedItems.isEmpty()) {

                return;
            }
            if (checkListViewProps.containsKey(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP)) {
                checkListViewProps.remove(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP);

                return;
            }
            while (change.next()) {
                if (!change.wasAdded()) {
                    continue;
                }
                addedSubList = change.getAddedSubList();
                if (addedSubList.contains(SELECT_ALL_ITEM)) {
                    checkAllAdded = true;
                } else {
                    otherAdded = true;
                }
            }
            if (checkAllAdded && checkModel.getItemCount() > 1) {
                checkListViewProps.put(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP, true);
                checkModel.clearChecks();
                checkListViewProps.put(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP, true);
                checkModel.check(SELECT_ALL_ITEM);
            } else if (otherAdded && checkModel.isChecked(SELECT_ALL_ITEM)) {
                checkListViewProps.put(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP, true);
                checkModel.clearCheck(SELECT_ALL_ITEM);
            }
        });

        resetBtn.setFocusTraversable(false);
        resetBtn.setOnAction(evt -> {
            checkModel.clearChecks();
            checkListView.getProperties().put(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP, true);
            checkModel.check(SELECT_ALL_ITEM);
        });
        resetBtn.disableProperty().bind(searchLoadingProperty);
        okBtn.setFocusTraversable(false);
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(evt -> {
            Set<String> checkedKeys = checkModel.getCheckedItems().stream()
                    .map(sourceAndMovieCountPair -> sourceAndMovieCountPair.getSourceBean().getKey())
                    .collect(Collectors.toSet());

            onFilter.accept(checkedKeys);
            hide();
        });
        okBtn.disableProperty().bind(searchLoadingProperty.or(Bindings.isEmpty(checkedItems)));
        closeBtn.setGraphic(FontIcon.of(FontAwesome.WINDOW_CLOSE, 16, Color.RED));
        closeBtn.setFocusTraversable(false);
        closeBtn.setOnAction(evt -> hide());
        btnHBox = new HBox(closeBtn, resetBtn, okBtn);
        btnHBox.setSpacing(5);
        btnHBox.setAlignment(Pos.BASELINE_RIGHT);

        sidebarContent = new VBox(5);
        sidebarContent.setPadding(new Insets(15, 10, 10, 10));
        sidebarContent.getChildren().addAll(checkListView, btnHBox);

        tipLabel.textProperty().bind(
                Bindings.when(searchLoadingProperty)
                        .then(I18nHelper.get(I18nKeys.TV_SOURCE_FILTER_SEARCHING))
                        .otherwise(I18nHelper.get(I18nKeys.TV_SOURCE_FILTER_SEARCHING_FINISHED))
        );
        tipLabelStyleClasses = tipLabel.getStyleClass();
        tipLabelStyleClasses.add("fs-big");
        tipLabelStyleClasses.add("bold");
        tipLabelStyleClasses.add("dark-orange");
        // 侧边栏面板（白底）
        sidebar = new VBox(50);
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setMinWidth(SIDEBAR_WIDTH);
        sidebar.setMaxWidth(SIDEBAR_WIDTH);
        sidebar.getStyleClass().add("source-filter-slide-sidebar");
        sidebar.getChildren().addAll(sidebarContent, new StackPane(tipLabel));

        getChildren().add(sidebar);
        StackPane.setAlignment(sidebar, Pos.CENTER_RIGHT);
        setPickOnBounds(false);
        setVisible(false);
        setManaged(false);

        // 动画
        slideIn = new TranslateTransition(ANIM_DURATION, sidebar);
        slideIn.setFromX(SIDEBAR_WIDTH);
        slideIn.setToX(0);
        slideIn.setOnFinished(evt -> sidebarShowing = true);

        slideOut = new TranslateTransition(ANIM_DURATION, sidebar);
        slideOut.setFromX(0);
        slideOut.setToX(SIDEBAR_WIDTH);
        slideOut.setOnFinished(evt -> {
            sidebarShowing = false;
            setVisible(false);
            setManaged(false);
        });
    }

    /**
     * 添加源站点
     * @param sourceAndMovieCount 源站点
     */
    public void addSourceBean(SourceBeanCountItem sourceAndMovieCount) {
        ObservableList<SourceBeanCountItem> items = checkListView.getItems();

        for (SourceBeanCountItem item : items) {
            if (item.getSourceBean().getKey().equals(sourceAndMovieCount.getSourceBean().getKey())) {

                return;
            }
        }
        items.add(sourceAndMovieCount);
        SELECT_ALL_ITEM.setMovieCount(SELECT_ALL_ITEM.getMovieCount() + sourceAndMovieCount.getMovieCount());
    }

    /**
     * 清空源站点列表
     */
    public void clearSourceBeans() {
        ObservableList<SourceBeanCountItem> items = checkListView.getItems();
        IndexedCheckModel<SourceBeanCountItem> checkModel = checkListView.getCheckModel();

        checkModel.clearChecks();
        items.clear();
        SELECT_ALL_ITEM.setMovieCount(0);
        items.add(SELECT_ALL_ITEM);
        checkModel.check(SELECT_ALL_ITEM);
    }

    /**
     * 获取当前选中的源站点key集合
     */
    public boolean hasCheckedKey(String sourceKey) {
        if (isCheckedAll()) {

            return true;
        }
        for (SourceBeanCountItem sourceAndMovieCount : checkListView.getCheckModel().getCheckedItems()) {
            if (sourceKey.equals(sourceAndMovieCount.getSourceBean().getKey())) {

                return true;
            }
        }

        return false;
    }

    /**
     * 显示侧边栏
     *
     * @param parentWidth  父容器宽度
     * @param parentHeight 父容器高度
     */
    public void show(double parentWidth, double parentHeight) {
        if (sidebarShowing) {

            return;
        }
        setVisible(true);
        setManaged(true);
        setPrefSize(parentWidth, parentHeight);
        sidebar.setPrefHeight(parentHeight);
        sidebar.setTranslateX(SIDEBAR_WIDTH);
        slideIn.playFromStart();
    }

    /**
     * 隐藏侧边栏
     */
    public void hide() {
        if (!sidebarShowing) {

            return;
        }
        slideOut.playFromStart();
    }

    /**
     * 将此侧边栏附加到父容器中
     *
     * @param parent 父容器，必须是一个 StackPane
     */
    public void attachTo(StackPane parent) {
        parent.getChildren().add(this);
    }

    /**
     * 源站点列表是否为空
     * @return bool
     */
    public boolean isEmpty() {
        ObservableList<SourceBeanCountItem> items = checkListView.getItems();

        return items.size() == 1 && items.get(0) == SELECT_ALL_ITEM;
    }

    /**
     * 是否全选
     * @return 是否全选
     */
    public boolean isCheckedAll() {
        return !checkListView.getItems().isEmpty() && checkListView.getCheckModel().isChecked(SELECT_ALL_ITEM);
    }
}
