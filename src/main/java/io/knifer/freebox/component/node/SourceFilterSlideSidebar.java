package io.knifer.freebox.component.node;

import io.knifer.freebox.component.factory.SourceBeanCheckListCellFactory;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.common.tvbox.SourceBean;
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
    private final CheckListView<SourceBean> checkListView;
    private final TranslateTransition slideIn;
    private final TranslateTransition slideOut;
    /**
     * 侧边栏是否正在显示
     */
    @Getter
    private boolean sidebarShowing = false;

    private static final double SIDEBAR_WIDTH = 240.0;
    private static final Duration ANIM_DURATION = Duration.millis(250);
    private static final SourceBean SELECT_ALL_SOURCE_BEAN;
    public static final String SELECT_ALL_SOURCE_KEY = "SELECT_ALL@FREE_BOX";
    private static final String SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP = "skip_checked_items_listener";

    static {
        SELECT_ALL_SOURCE_BEAN = new SourceBean();
        SELECT_ALL_SOURCE_BEAN.setKey(SELECT_ALL_SOURCE_KEY);
        SELECT_ALL_SOURCE_BEAN.setName(I18nHelper.get(I18nKeys.COMMON_SELECT_ALL));
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
        CheckModel<SourceBean> checkModel;
        SourceBeanCheckListCellFactory cellFactory;
        ObservableMap<Object, Object> checkListViewProps;
        BooleanProperty checkableProperty;
        ObservableList<SourceBean> checkedItems;

        // 侧边栏主体
        checkListView = new CheckListView<>();
        checkListView.setFocusTraversable(false);
        VBox.setVgrow(checkListView, Priority.ALWAYS);
        cellFactory = new SourceBeanCheckListCellFactory(checkListView, onSelect);
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
                checkModel.check(SELECT_ALL_SOURCE_BEAN);
            } else {
                NodeUtil.replaceStyleClass(tipLabel, "success", "dark-orange");
            }
        });
        checkableProperty.bind(searchLoadingProperty.not());
        checkListView.setCellFactory(cellFactory);
        checkedItems = checkModel.getCheckedItems();
        checkedItems.addListener((ListChangeListener<SourceBean>) change -> {
            List<? extends SourceBean> addedSubList;
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
                if (addedSubList.contains(SELECT_ALL_SOURCE_BEAN)) {
                    checkAllAdded = true;
                } else {
                    otherAdded = true;
                }
            }
            if (checkAllAdded && checkModel.getItemCount() > 1) {
                checkListViewProps.put(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP, true);
                checkModel.clearChecks();
                checkListViewProps.put(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP, true);
                checkModel.check(SELECT_ALL_SOURCE_BEAN);
            } else if (otherAdded && checkModel.isChecked(SELECT_ALL_SOURCE_BEAN)) {
                checkListViewProps.put(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP, true);
                checkModel.clearCheck(SELECT_ALL_SOURCE_BEAN);
            }
        });

        resetBtn.setFocusTraversable(false);
        resetBtn.setOnAction(evt -> {
            checkModel.clearChecks();
            checkListView.getProperties().put(SKIP_CHECKED_ITEMS_LISTENER_FLAG_PROP, true);
            checkModel.check(SELECT_ALL_SOURCE_BEAN);
        });
        resetBtn.disableProperty().bind(searchLoadingProperty);
        okBtn.setFocusTraversable(false);
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(evt -> {
            Set<String> checkedKeys = checkModel.getCheckedItems().stream()
                    .map(SourceBean::getKey)
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
     * @param sourceBean 源站点
     */
    public void addSourceBean(SourceBean sourceBean) {
        ObservableList<SourceBean> items = checkListView.getItems();

        for (SourceBean item : items) {
            if (item.getKey().equals(sourceBean.getKey())) {

                return;
            }
        }
        items.add(sourceBean);
    }

    /**
     * 清空源站点列表
     */
    public void clearSourceBeans() {
        ObservableList<SourceBean> items = checkListView.getItems();
        IndexedCheckModel<SourceBean> checkModel = checkListView.getCheckModel();

        checkModel.clearChecks();
        items.clear();
        items.add(SELECT_ALL_SOURCE_BEAN);
        checkModel.check(SELECT_ALL_SOURCE_BEAN);
    }

    /**
     * 获取当前选中的源站点key集合
     */
    public boolean hasCheckedKey(String sourceKey) {
        if (isCheckedAll()) {

            return true;
        }
        for (SourceBean sourceBean : checkListView.getCheckModel().getCheckedItems()) {
            if (sourceKey.equals(sourceBean.getKey())) {

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
        ObservableList<SourceBean> items = checkListView.getItems();

        return items.size() == 1 && items.get(0) == SELECT_ALL_SOURCE_BEAN;
    }

    /**
     * 是否全选
     * @return 是否全选
     */
    public boolean isCheckedAll() {
        return !checkListView.getItems().isEmpty() && checkListView.getCheckModel().isChecked(SELECT_ALL_SOURCE_BEAN);
    }
}
