package io.knifer.freebox.controller;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.watchers.DelayWatcher;
import io.knifer.freebox.component.router.Router;
import io.knifer.freebox.constant.*;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.controller.spiderDebugging.*;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.domain.MovieSortFilterTreeNode;
import io.knifer.freebox.model.domain.SpiderDebugging;
import io.knifer.freebox.spider.js.JSSpider;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CastUtil;
import jakarta.inject.Inject;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.*;
import org.graalvm.polyglot.PolyglotException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 爬虫调试
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public class SpiderDebuggingController {

    @FXML
    private BorderPane root;
    @FXML
    private VBox tabPaneVBox;
    @FXML
    private VBox emptyDataPlaceholder;
    @FXML
    private Button importButton;
    @FXML
    private Button deleteButton;
    @FXML
    private ComboBox<SpiderDebugging> spiderSelectComboBox;
    @FXML
    private ProgressIndicator spiderLoadingProgressIndicator;
    @FXML
    private Label spiderStatusLabel;
    @FXML
    @Getter
    private TabPane previewTabPane;
    @FXML
    private HBox dataPanelToolBar;
    @FXML
    private StackPane dataPanel;
    @FXML
    private HBox dataPanelDragHandle;
    @FXML
    private StackPane previewStackPane;
    @FXML
    private TextArea debugDataTextArea;

    @Setter
    @Getter
    private HomeTabController homeTabController;
    @Setter
    @Getter
    private MovieExploreTabController movieExploreTabController;
    @Setter
    @Getter
    private MovieDetailTabController movieDetailTabController;
    @Setter
    @Getter
    private MoviePlayTabController moviePlayTabController;
    @Setter
    @Getter
    private MovieSearchTabController movieSearchTabController;

    private Stage stage;
    private FileChooser spiderFileChooser;
    @Getter
    private BooleanProperty spiderLoadingProperty;

    @Getter
    private ExecutorService spiderPreviewExecutor;
    private Set<SourceAuditType> tabTypeUpdatedSet;

    private double dataPanelHeight = 200.0;
    private double dataPanelDragStartY;
    private double dataPanelDragStartHeight;

    /***
     * 上锁资源
     ***/
    private volatile JSSpider spider;
    private volatile boolean spiderAvailableFlag;
    @Getter
    private Map<SourceAuditType, Future<?>> spiderPreviewTaskMap;
    private Map<SourceAuditType, String> debugDataMap;

    private volatile WatchMonitor fileMonitor;

    private final ReentrantLock spiderLock = new ReentrantLock();
    private final AtomicBoolean spiderInitializing = new AtomicBoolean(false);
    private final ReentrantLock fileMonitorLock = new ReentrantLock();
    private final AtomicBoolean fileMonitorInitializing = new AtomicBoolean(false);

    private final Router router;
    private final Context context;

    private final static String SPIDER_SELECT_COMBOBOX_ACTION_FLAG = "actionFlag";
    private final static double DEBUG_PANEL_MIN_HEIGHT = 100.0;
    private final static double DEBUG_PANEL_MAX_RATIO = 0.8;

    @FXML
    private void initialize() {
        ObjectProperty<SpiderDebugging> spiderSelectValueProperty = spiderSelectComboBox.valueProperty();
        BooleanBinding spiderSelectValueIsNull = spiderSelectValueProperty.isNull();
        BooleanBinding spiderSelectValueIsNotNull = spiderSelectValueProperty.isNotNull();

        spiderPreviewExecutor = new ThreadPoolExecutor(
                1, 1, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r);

                    t.setName("Spider-Preview-Thread");
                    t.setUncaughtExceptionHandler(GlobalExceptionHandler.getInstance());

                    return t;
                }
        );
        spiderPreviewTaskMap = new ConcurrentHashMap<>();
        debugDataMap = new ConcurrentHashMap<>();
        tabTypeUpdatedSet = new ConcurrentHashSet<>();

        spiderLoadingProperty = new SimpleBooleanProperty(false);

        emptyDataPlaceholder.visibleProperty().bind(spiderSelectValueIsNull);
        tabPaneVBox.visibleProperty().bind(spiderSelectValueIsNotNull);
        spiderStatusLabel.visibleProperty().bind(spiderSelectValueIsNotNull);
        spiderLoadingProgressIndicator.visibleProperty().bind(spiderLoadingProperty);
        importButton.disableProperty().bind(spiderLoadingProperty);
        deleteButton.disableProperty().bind(spiderSelectValueIsNull);
        spiderSelectComboBox.disableProperty().bind(spiderLoadingProperty);
        previewTabPane.disableProperty().bind(spiderLoadingProperty);

        initDebugPanelHoverEffect();
        initDebugPanelResizeHandler();
        previewStackPane.heightProperty().addListener((obs, oldH, newH) -> {
            double maxHeight = newH.doubleValue() * DEBUG_PANEL_MAX_RATIO;

            if (dataPanel.isVisible() && dataPanel.getHeight() > maxHeight) {
                dataPanel.setPrefHeight(maxHeight);
                dataPanel.setMaxHeight(maxHeight);
                dataPanelHeight = maxHeight;
            }
        });

        context.registerEventListener(
                AppEvents.SpiderDebuggingViewTabLoaded.class,
                evt -> {
                    SourceAuditType type = evt.tabType();
                    String loadedData = evt.loadedData();

                    tabTypeUpdatedSet.add(type);
                    debugDataMap.put(type, ObjectUtils.defaultIfNull(loadedData, StringUtils.EMPTY));
                    if (getCurrentTabType() == type) {
                        setDebugData(loadedData);
                    }
                }
        );
        context.postEvent(new AppEvents.SpiderDebuggingViewInitialized(this));

        loadData();

        Platform.runLater(() -> {
            stage = WindowHelper.getStage(root);
            stage.setOnHidden(evt -> {
                AsyncUtil.execute(() -> {
                    spiderPreviewExecutor.shutdownNow();
                    destroyFileMonitorIfExists();
                    destroySpiderIfExists();
                });
                router.back();
            });
            spiderSelectComboBox.maxWidthProperty().bind(stage.widthProperty().divide(4));
        });
    }

    private void loadData() {
        AsyncUtil.execute(() -> {
            Map<String, SpiderDebugging> spiderDebuggingMap = StorageHelper.findAll(SpiderDebugging.class);

            if (spiderDebuggingMap.isEmpty()) {

                return;
            }
            Platform.runLater(() -> spiderSelectComboBox.getItems().addAll(spiderDebuggingMap.values()));
        });
    }

    @FXML
    private void onImportButtonAction() {
        // 选择爬虫文件
        File spiderFile = selectSpiderFile();
        SpiderDebugging spiderDebugging;

        if (spiderFile == null) {

            return;
        }
        spiderLoadingProperty.set(true);
        previewTabPane.getSelectionModel().select(SpiderDebuggingTabController.HOME_TAB_IDX);
        tabTypeUpdatedSet.addAll(Arrays.asList(SourceAuditType.values()));
        // 爬虫初始化
        spiderDebugging = SpiderDebugging.from(spiderFile);
        AsyncUtil.execute(() -> {
            if (StorageHelper.find(spiderDebugging.getId(), SpiderDebugging.class).isEmpty()) {
                // 如果爬虫未被导入过，保存爬虫数据并初始化
                StorageHelper.save(spiderDebugging);
                initFileMonitor(spiderDebugging);
                initSpider(spiderDebugging);
            } else {
                // 如果爬虫被导入过，直接选中（initSpider方法会在选中事件触发后调用）
                Platform.runLater(() -> {
                    clearUI(true);
                    updateSpiderDebugging(spiderDebugging);
                });
            }
        });
    }

    @Nullable
    private File selectSpiderFile() {
        File result;

        if (spiderFileChooser == null) {
            spiderFileChooser = new FileChooser();
            spiderFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Javascript", "*.js"
            ));
        }
        result = spiderFileChooser.showOpenDialog(stage);
        log.info("select source code file: {}", result == null ? "null" : result.getAbsolutePath());

        return result;
    }

    private void initFileMonitor(SpiderDebugging spiderDebugging) {
        WatchMonitor newMonitor;

        if (!fileMonitorInitializing.compareAndSet(false, true)) {
            // 有其他线程在初始化文件监控，取消当前初始化动作
            log.warn("Another file monitor init in progress, cancelling");

            return;
        }
        try {
            destroyFileMonitorIfExists();
            newMonitor = createSpiderFileMonitor(spiderDebugging);
        } finally {
            fileMonitorInitializing.set(false);
        }
        fileMonitorLock.lock();
        try {
            fileMonitor = newMonitor;
            fileMonitor.start();
        } finally {
            fileMonitorLock.unlock();
            log.info("file monitor started");
        }
    }

    /**
     * 初始化spider
     * @param spiderDebugging spider信息
     */
    private void initSpider(SpiderDebugging spiderDebugging) {
        JSSpider newSpider;
        boolean spiderInitSuccess;

        if (!spiderInitializing.compareAndSet(false, true)) {
            // 有其他线程在初始化爬虫，取消当前初始化动作
            log.warn("Another spider init in progress, cancelling");
            Platform.runLater(() -> spiderLoadingProperty.set(false));

            return;
        }
        try {
            destroySpiderIfExists();
            newSpider = new JSSpider(StringUtils.EMPTY, Path.of(spiderDebugging.getSourceFilePath()));
            try {
                newSpider.init(StringUtils.EMPTY);
                spiderInitSuccess = true;
            } catch (Exception e) {
                if (tryHandleExecutionInterrupt(e)) {

                    return;
                }
                log.error("spider init exception", e);
                spiderInitSuccess = false;
            }
            spiderLock.lock();
            try {
                spiderAvailableFlag = spiderInitSuccess;
                spider = newSpider;
            } finally {
                spiderLock.unlock();
            }
            Platform.runLater(() -> {
                SourceAuditType currentTabType;
                SpiderDebuggingTabController childController;

                if (spiderAvailableFlag) {
                    setSpiderStatus(true, I18nKeys.SPIDER_DEBUGGING_SPIDER_MONITORING);
                    updateSpiderDebugging(spiderDebugging);
                    spiderLoadingProperty.set(false);
                } else {
                    clearUI(false);
                    setSpiderStatus(
                            false, I18nKeys.SPIDER_DEBUGGING_SPIDER_RUNNING_ERROR
                    );
                }
                // 根据用户所处的tab页，刷新对应tab的数据
                tabTypeUpdatedSet.clear();
                currentTabType = getCurrentTabType();
                if (
                        currentTabType == SourceAuditType.HOME ||
                        (
                                currentTabType == SourceAuditType.MOVIE_EXPLORE &&
                                movieExploreTabController.isAutoRefreshOn()
                        )
                ) {
                    // 因为movie explore tab依赖于home tab的影视分类数据，所以刷新home tab即可
                    // home tab 的controller将会对movie explore tab进行判断和刷新
                    homeTabController.reload();
                } else if ((childController = getTabControllerByType(currentTabType)).isAutoRefreshOn()) {
                    childController.reload();
                }
            });
        } finally {
            spiderInitializing.set(false);
        }
    }

    private SourceAuditType getCurrentTabType() {
        Tab currentTab = previewTabPane.getSelectionModel().getSelectedItem();

        return CastUtil.cast(currentTab.getProperties().get("type"));
    }

    /**
     * 移除爬虫监控状态（UI）
     */
    private void clearSpiderStatus() {
        ObservableList<String> styleClasses = spiderStatusLabel.getStyleClass();

        spiderStatusLabel.setText(StringUtils.EMPTY);
        styleClasses.remove("danger");
        styleClasses.remove("success");
    }

    /**
     * 设置爬虫状态（UI）
     * @param successFlag 是否初始化成功
     */
    private void setSpiderStatus(boolean successFlag, String textI18n) {
        ObservableList<String> styleClasses = spiderStatusLabel.getStyleClass();

        if (successFlag) {
            styleClasses.remove("danger");
            styleClasses.add("success");
        } else {
            styleClasses.remove("success");
            styleClasses.add("danger");
        }
        spiderStatusLabel.setText(I18nHelper.get(textI18n));
    }

    private WatchMonitor createSpiderFileMonitor(SpiderDebugging spiderDebugging) {
        WatchMonitor newMonitor = WatchMonitor.create(
                spiderDebugging.getSourceFilePath(), WatchMonitor.ENTRY_MODIFY, WatchMonitor.ENTRY_DELETE
                ).setWatcher(new DelayWatcher(new SimpleWatcher() {

                    @Override
                    public void onModify(WatchEvent<?> event, Path currentPath) {
                        log.debug("spider file modified: {}", currentPath);
                        initSpider(spiderDebugging);
                    }

                    @Override
                    public void onDelete(WatchEvent<?> event, Path currentPath) {
                        log.info("spider file deleted: {}", currentPath);
                        Platform.runLater(() -> {
                            ToastHelper.showErrorI18n(I18nKeys.SPIDER_DEBUGGING_SPIDER_FILE_NOT_EXISTS);
                            clearUI(false);
                        });
                    }
                }, 500));
        newMonitor.setUncaughtExceptionHandler(GlobalExceptionHandler.getInstance());
        newMonitor.setName("Spider File Monitor Thread");

        return newMonitor;
    }

    /**
     * 选中对应的调试爬虫。如果传入的爬虫不存在，则在ComboBox中新建。
     * @param spiderDebugging 调试爬虫数据
     */
    private void updateSpiderDebugging(SpiderDebugging spiderDebugging) {
        List<SpiderDebugging> items = spiderSelectComboBox.getItems();
        Map<Object, Object> properties;

        if (!items.contains(spiderDebugging)) {
            items.add(spiderDebugging);
        }
        properties = spiderSelectComboBox.getProperties();
        if (properties.containsKey(SPIDER_SELECT_COMBOBOX_ACTION_FLAG)) {
            properties.remove(SPIDER_SELECT_COMBOBOX_ACTION_FLAG);
        } else {
            spiderSelectComboBox.getSelectionModel().select(spiderDebugging);
        }
        stage.setTitle(String.format(
                "%s - %s", I18nHelper.get(I18nKeys.SPIDER_DEBUGGING), spiderDebugging.getSourceFilePath()
        ));
    }

    /**
     * 销毁爬虫
     */
    private void destroySpiderIfExists() {
        Collection<Future<?>> tasks = spiderPreviewTaskMap.values();
        JSSpider localSpider;

        spiderPreviewTaskMap.clear();
        if (!tasks.isEmpty()) {
            tasks.stream()
                    .filter(t -> !t.isDone())
                    .forEach(t -> t.cancel(true));
        }
        spiderLock.lock();
        try {
            localSpider = spider;
            if (localSpider != null) {
                localSpider.destroy();
                spider = null;
                log.info("spider destroyed");
            }
        } finally {
            spiderLock.unlock();
        }
    }

    /**
     * 销毁爬虫文件监视器
     */
    private void destroyFileMonitorIfExists() {
        WatchMonitor localMonitor;

        fileMonitorLock.lock();
        try {
            localMonitor = fileMonitor;
            fileMonitor = null;
        } finally {
            fileMonitorLock.unlock();
        }
        if (localMonitor != null) {
            localMonitor.close();
            log.info("file monitor destroyed");
        }
    }

    /**
     * 如果可以，处理爬虫中断异常。
     * 爬虫中断是预期场景，当用户切换、删除爬虫脚本时，就会发生爬虫中断。
     * @param e 异常
     * @return 是否为爬虫中断异常
     */
    public boolean tryHandleExecutionInterrupt(Exception e) {
        String message = e.getMessage();

        if (
                e instanceof InterruptedException ||
                e instanceof InterruptedIOException ||
                e instanceof PolyglotException && (
                        "Context execution was cancelled.".equals(message) ||
                        "Execution got interrupted.".equals(message)
                )
        ) {
            log.debug("user cancelled execution", e);

            return true;
        }

        return false;
    }

    @FXML
    private void onOpenLogConsoleButtonAction() {
        router.openSecondary(Views.LOG_CONSOLE_DIALOG, I18nKeys.SETTINGS_DEBUGGING_LOG_CONSOLE);
    }

    @FXML
    private void onCloseDebugPanelButtonAction() {
        dataPanel.setVisible(false);
        dataPanel.setManaged(false);
    }

    @FXML
    private void onCopyDataPanelButtonAction() {
        String text = debugDataTextArea.getText();

        if (StringUtils.isNotEmpty(text)) {
            ClipboardHelper.setContent(text);
            ToastHelper.showMouseToastI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
        }
    }

    @FXML
    private void onToggleDebugPanelButtonAction() {
        boolean showing = dataPanel.isVisible();

        if (showing) {
            dataPanel.setVisible(false);
            dataPanel.setManaged(false);
        } else {
            dataPanel.setManaged(true);
            dataPanel.setVisible(true);
            dataPanel.setPrefHeight(dataPanelHeight);
            dataPanel.setMaxHeight(dataPanelHeight);
        }
    }

    private void initDebugPanelHoverEffect() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), dataPanelToolBar);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), dataPanelToolBar);

        fadeIn.setToValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(evt -> dataPanelToolBar.setMouseTransparent(true));

        dataPanelToolBar.setOpacity(0);
        dataPanelToolBar.setMouseTransparent(true);

        dataPanel.setOnMouseEntered(evt -> {
            dataPanelToolBar.setMouseTransparent(false);
            fadeOut.stop();
            fadeIn.playFromStart();
        });
        dataPanel.setOnMouseExited(evt -> {
            fadeIn.stop();
            fadeOut.playFromStart();
        });
    }

    private void initDebugPanelResizeHandler() {
        dataPanelDragHandle.setOnMousePressed(evt -> {
            dataPanelDragStartY = evt.getSceneY();
            dataPanelDragStartHeight = dataPanel.getHeight();
            evt.consume();
        });
        dataPanelDragHandle.setOnMouseDragged(evt -> {
            double deltaY = dataPanelDragStartY - evt.getSceneY();
            double newHeight = dataPanelDragStartHeight + deltaY;
            double maxHeight = previewStackPane.getHeight() * DEBUG_PANEL_MAX_RATIO;

            newHeight = Math.max(DEBUG_PANEL_MIN_HEIGHT, Math.min(maxHeight, newHeight));
            dataPanel.setPrefHeight(newHeight);
            dataPanel.setMaxHeight(newHeight);
            dataPanelHeight = newHeight;
            evt.consume();
        });
    }

    private void setDebugData(String text) {
        debugDataTextArea.setText(text);
    }

    @FXML
    private void onSpiderSelectComboBoxAction() {
        SpiderDebugging spiderDebugging = spiderSelectComboBox.getValue();
        File spiderFile;

        if (spiderDebugging == null) {

            return;
        }
        spiderLoadingProperty.set(true);
        spiderFile = new File(spiderDebugging.getSourceFilePath());
        if (!spiderFile.exists()) {
            ToastHelper.showErrorI18n(I18nKeys.SPIDER_DEBUGGING_SPIDER_FILE_NOT_EXISTS);
            clearUI(false);

            return;
        }
        spiderSelectComboBox.getProperties().put(SPIDER_SELECT_COMBOBOX_ACTION_FLAG, true);
        AsyncUtil.execute(() -> {
            initFileMonitor(spiderDebugging);
            initSpider(spiderDebugging);
        });
    }

    @FXML
    private void onDeleteButtonAction() {
        SpiderDebugging spiderDebugging = spiderSelectComboBox.getValue();

        if (spiderDebugging == null) {

            return;
        }
        ToastHelper.showConfirmI18n(
                I18nKeys.COMMON_HINT, I18nKeys.SPIDER_DEBUGGING_DELETE_CONFIRM, flag -> {
                    if (!flag) {

                        return;
                    }
                    spiderSelectComboBox.getItems().remove(spiderDebugging);
                    clearUI(true);
                    ToastHelper.showSuccessI18n(I18nKeys.COMMON_MESSAGE_SUCCESS);
                    AsyncUtil.execute(() -> {
                        log.info("delete spider debugging: {}", spiderDebugging);
                        StorageHelper.delete(spiderDebugging);
                        destroySpiderIfExists();
                    });
                }
        );
    }

    /**
     * 清空UI
     * @param clearSpiderSelection 是否清空已选择的爬虫
     */
    private void clearUI(boolean clearSpiderSelection) {
        if (clearSpiderSelection) {
            spiderSelectComboBox.getSelectionModel().clearSelection();
        }
        homeTabController.clear();
        movieExploreTabController.clear();
        movieDetailTabController.clear();
        clearSpiderStatus();
        spiderLoadingProperty.set(false);
        stage.setTitle(I18nHelper.get(I18nKeys.SPIDER_DEBUGGING));
    }

    public void cancelSpiderTaskIfNeeded(Future<?> task) {
        JSSpider spider;

        if (task.isDone()) {

            return;
        }
        task.cancel(true);
        spider = requireSpider();
        if (spider == null) {

            return;
        }
        spider.interrupt();
    }

    @Nullable
    public JSSpider requireSpider() {
        JSSpider localSpider;

        spiderLock.lock();
        try {
            localSpider = spider;
        } finally {
            spiderLock.unlock();
        }

        return localSpider;
    }

    /**
     * 将一棵影视参数筛选树的筛选数据复制到另一棵上
     * @param sourceTreeView 源
     * @param targetTreeView 目标
     */
    public void copyMovieSortFilterCheckTreeViewChecks(
            CheckTreeView<MovieSortFilterTreeNode> sourceTreeView,
            CheckTreeView<MovieSortFilterTreeNode> targetTreeView
    ) {
        CheckModel<TreeItem<MovieSortFilterTreeNode>> targetCheckModel = targetTreeView.getCheckModel();
        CheckModel<TreeItem<MovieSortFilterTreeNode>> sourceCheckModel = sourceTreeView.getCheckModel();
        ObservableList<TreeItem<MovieSortFilterTreeNode>> checkedItems = sourceCheckModel.getCheckedItems();
        ObservableList<TreeItem<MovieSortFilterTreeNode>> targetTreeParentItems;
        MovieSortFilterTreeNode sourceFilter;
        MovieSortFilterTreeNode targetFilterParent;

        targetCheckModel.clearChecks();
        if (checkedItems.isEmpty() || (targetTreeParentItems = targetTreeView.getRoot().getChildren()).isEmpty()) {

            return;
        }
        for (TreeItem<MovieSortFilterTreeNode> item : checkedItems) {
            sourceFilter = item.getValue();
            for (TreeItem<MovieSortFilterTreeNode> targetFilterParentItem : targetTreeParentItems) {
                targetFilterParent = targetFilterParentItem.getValue();
                if (!Objects.equals(sourceFilter.getFilterKey(), targetFilterParent.getFilterKey())) {
                    continue;
                }
                for (TreeItem<MovieSortFilterTreeNode> targetFilterItem : targetFilterParentItem.getChildren()) {
                    if (Objects.equals(sourceFilter.getFilterValue(), targetFilterItem.getValue().getFilterValue())) {
                        targetCheckModel.check(targetFilterItem);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 将指定影视ID发送到影视详情tab
     * @param movieId 影视ID
     */
    public void sendToMovieDetailTab(String movieId) {
        if (StringUtils.isEmpty(movieId)) {
            ToastHelper.showWarningI18n(I18nKeys.SPIDER_DEBUGGING_COMMON_MOVIE_ID_REQUIRED);

            return;
        }
        movieDetailTabController.getMovieIdTextField().setText(movieId);
        movieDetailTabController.reload();
        previewTabPane.getSelectionModel().select(SpiderDebuggingTabController.MOVIE_DETAIL_TAB_IDX);
    }

    /**
     * 将指定剧集ID发送到影视播放tab
     * @param vodId 剧集ID
     */
    public void sendToMoviePlayTab(String vodId) {
        if (StringUtils.isEmpty(vodId)) {
            ToastHelper.showWarningI18n(I18nKeys.SPIDER_DEBUGGING_MOVIE_DETAIL_VOD_ID_REQUIRED);

            return;
        }
        moviePlayTabController.getVodIdTextField().setText(vodId);
        moviePlayTabController.reload();
        previewTabPane.getSelectionModel().select(SpiderDebuggingTabController.MOVIE_PLAY_TAB_IDX);
    }

    @FXML
    private void onPreviewTabSelectionChanged(Event event) {
        Tab tab;
        SourceAuditType tabType;
        SpiderDebuggingTabController childController;
        Future<?> runningTask;

        if (tabTypeUpdatedSet == null) {
            // 还没进行过initialize，忽略事件

            return;
        }
        tab = CastUtil.cast(event.getSource());
        if (!tab.isSelected()) {

            return;
        }
        tabType = CastUtil.cast(tab.getProperties().get("type"));
        setDebugData(debugDataMap.get(tabType));
        if (tabTypeUpdatedSet.contains(tabType)) {

            return;
        }
        childController = getTabControllerByType(tabType);
        runningTask = spiderPreviewTaskMap.get(tabType);
        if (runningTask != null && !runningTask.isDone()) {

            return;
        }
        if (childController.isAutoRefreshOn()) {
            childController.reload();
        }
    }

    private SpiderDebuggingTabController getTabControllerByType(SourceAuditType type) {
        return switch (type) {
            case HOME -> homeTabController;
            case MOVIE_EXPLORE -> movieExploreTabController;
            case MOVIE_DETAIL -> movieDetailTabController;
            case MOVIE_PLAY -> moviePlayTabController;
            case MOVIE_SEARCH -> movieSearchTabController;
        };
    }
}
