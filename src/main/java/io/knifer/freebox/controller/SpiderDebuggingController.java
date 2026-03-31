package io.knifer.freebox.controller;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.watchers.DelayWatcher;
import io.knifer.freebox.component.node.MovieSortFilterCheckBoxTreeItem;
import io.knifer.freebox.component.router.Router;
import io.knifer.freebox.constant.*;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.common.catvod.Result;
import io.knifer.freebox.model.common.tvbox.AbsSortXml;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.model.common.tvbox.MovieSort;
import io.knifer.freebox.model.domain.MovieSortFilterTreeNode;
import io.knifer.freebox.model.domain.SpiderDebugging;
import io.knifer.freebox.net.websocket.converter.CatVodBeanConverter;
import io.knifer.freebox.spider.js.JSSpider;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.json.GsonUtil;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckTreeView;
import org.controlsfx.control.InfoOverlay;
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
    private Label spiderMonitoringStatusLabel;
    @FXML
    private TabPane previewTabPane;
    @FXML
    private BorderPane homeTabContentBorderPane;
    @FXML
    private ProgressIndicator homeTabLoadingProgressIndicator;
    @FXML
    private ListView<MovieSort.SortData> classesListView;
    @FXML
    private HBox movieListHBox;
    @FXML
    private TextField movieClassIdTextField;
    @FXML
    private Label movieClassNameLabel;
    @FXML
    private CheckTreeView<MovieSortFilterTreeNode> movieSortFilterCheckTreeView;
    @FXML
    private Label movieNameLabel;
    @FXML
    private TextField movieIdTextField;
    @FXML
    private TextField moviePictureUrlTextField;

    private Stage stage;
    private FileChooser spiderFileChooser;
    private InfoOverlay lastSelectedMovieInfoOverlay;
    private BooleanProperty spiderLoadingProperty;
    private BooleanProperty homeTabLoadingProperty;

    private ExecutorService spiderPreviewExecutor;
    private Set<SourceAuditType> tabTypeUpdateSet;

    /***
     * 上锁资源
     ***/
    private JSSpider spider;
    private Future<?> spiderPreviewTask;
    private WatchMonitor spiderFileMonitor;

    private final ReentrantLock spiderLock = new ReentrantLock();
    private final AtomicBoolean spiderInitializing = new AtomicBoolean(false);

    private final CatVodBeanConverter beanConverter;
    private final Router router;

    private final static String SOURCE_KEY = "freebox_debugging";
    private final static double MOVIE_CELL_WIDTH = 150;
    private final static double MOVIE_CELL_HEIGHT = 200;
    private final static String SPIDER_SELECT_COMBOBOX_ACTION_FLAG = "actionFlag";

    @FXML
    private void initialize() {
        ObjectProperty<SpiderDebugging> spiderSelectValueProperty = spiderSelectComboBox.valueProperty();
        BooleanBinding spiderSelectValueIsNull = spiderSelectValueProperty.isNull();
        BooleanBinding spiderSelectValueIsNotNull = spiderSelectValueProperty.isNotNull();

        spiderPreviewExecutor = Executors.newSingleThreadExecutor();
        tabTypeUpdateSet = new ConcurrentHashSet<>();

        spiderLoadingProperty = new SimpleBooleanProperty(false);
        homeTabLoadingProperty = new SimpleBooleanProperty(false);
        emptyDataPlaceholder.visibleProperty().bind(spiderSelectValueIsNull);
        tabPaneVBox.visibleProperty().bind(spiderSelectValueIsNotNull);
        spiderMonitoringStatusLabel.visibleProperty().bind(spiderSelectValueIsNotNull);
        spiderLoadingProgressIndicator.visibleProperty().bind(spiderLoadingProperty);
        homeTabLoadingProgressIndicator.visibleProperty().bind(homeTabLoadingProperty.or(spiderLoadingProperty));
        homeTabContentBorderPane.visibleProperty().bind(homeTabLoadingProperty.not().and(spiderLoadingProperty.not()));
        importButton.disableProperty().bind(spiderLoadingProperty);
        deleteButton.disableProperty().bind(spiderSelectValueIsNull);
        spiderSelectComboBox.disableProperty().bind(spiderLoadingProperty);
        previewTabPane.disableProperty().bind(spiderLoadingProperty);

        loadData();

        Platform.runLater(() -> {
            stage = WindowHelper.getStage(root);
            stage.setOnHidden(evt -> {
                AsyncUtil.execute(() -> {
                    spiderPreviewExecutor.shutdownNow();
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
        previewTabPane.getSelectionModel().selectFirst();
        tabTypeUpdateSet.addAll(Arrays.asList(SourceAuditType.values()));
        // 爬虫初始化
        spiderDebugging = SpiderDebugging.from(spiderFile);
        AsyncUtil.execute(() -> {
            if (StorageHelper.find(spiderDebugging.getId(), SpiderDebugging.class).isEmpty()) {
                // 如果爬虫未被导入过，保存爬虫数据并初始化
                StorageHelper.save(spiderDebugging);
                initSpider(spiderFile, spiderDebugging);
            } else {
                // 如果爬虫被导入过，直接选中（initSpider方法会在选中事件触发后调用）
                Platform.runLater(() -> {
                    setSpiderMonitoringStatus(null);
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

    private void initSpider(File spiderFile, SpiderDebugging spiderDebugging) {
        AsyncUtil.execute(() -> {
            JSSpider newSpider;
            WatchMonitor newMonitor;

            if (!spiderInitializing.compareAndSet(false, true)) {
                // 有其他线程在初始化爬虫，取消当前初始化动作
                log.warn("Another init in progress, cancelling");
                Platform.runLater(() -> spiderLoadingProperty.set(false));

                return;
            }
            try {
                destroySpiderIfExists();
                newSpider = new JSSpider(StringUtils.EMPTY, spiderFile.toPath());
                try {
                    newSpider.init(StringUtils.EMPTY);
                    newMonitor = createSpiderFileMonitor(spiderFile);
                } catch (Exception e) {
                    if (!tryHandleExecutionInterrupt(e)) {
                        log.error("spider init exception", e);
                        Platform.runLater(() -> {
                            setSpiderMonitoringStatus(false);
                            ToastHelper.showErrorI18n(I18nKeys.SPIDER_DEBUGGING_SPIDER_MONITORING_ERROR);
                            spiderLoadingProperty.set(false);
                        });
                        destroySpiderIfExists();
                    }

                    return;
                }
                spiderLock.lock();
                try {
                    spider = newSpider;
                    spiderFileMonitor = newMonitor;
                    spiderFileMonitor.start();
                } finally {
                    spiderLock.unlock();
                }
                Platform.runLater(() -> {
                    setSpiderMonitoringStatus(true);
                    updateSpiderDebugging(spiderDebugging);
                    spiderLoadingProperty.set(false);
                });
                updateHomeTab();
            } finally {
                spiderInitializing.set(false);
            }
        });
    }

    /**
     * 设置爬虫监控状态UI
     * @param successFlag 是否监控成功（null=未监控，true=成功，false=失败）
     */
    private void setSpiderMonitoringStatus(@Nullable Boolean successFlag) {
        ObservableList<String> styleClasses = spiderMonitoringStatusLabel.getStyleClass();

        if (successFlag == null) {
            spiderMonitoringStatusLabel.setText(StringUtils.EMPTY);
            styleClasses.remove("danger");
            styleClasses.remove("success");
        } else if (successFlag) {
            spiderMonitoringStatusLabel.setText(I18nHelper.get(I18nKeys.SPIDER_DEBUGGING_SPIDER_MONITORING));
            styleClasses.remove("danger");
            styleClasses.add("success");
        } else {
            spiderMonitoringStatusLabel.setText(I18nHelper.get(
                    I18nKeys.SPIDER_DEBUGGING_SPIDER_MONITORING_ERROR
            ));
            styleClasses = spiderMonitoringStatusLabel.getStyleClass();
            styleClasses.remove("success");
            styleClasses.add("danger");
        }
    }

    private WatchMonitor createSpiderFileMonitor(File spiderFile) {
        WatchMonitor newMonitor = WatchMonitor.create(spiderFile, WatchMonitor.ENTRY_MODIFY, WatchMonitor.ENTRY_DELETE)

                .setWatcher(new DelayWatcher(new SimpleWatcher() {
                    @Override
                    public void onModify(WatchEvent<?> event, Path currentPath) {
                        log.info("spider file modified: {} - {}", currentPath, event);
                    }

                    @Override
                    public void onDelete(WatchEvent<?> event, Path currentPath) {
                        log.info("spider file deleted: {} - {}", currentPath, event);
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
            spiderSelectComboBox.getSelectionModel().selectLast();
        }
    }

    private void destroySpiderIfExists() {
        WatchMonitor localMonitor;
        Future<?> localTask;
        JSSpider localSpider;

        spiderLock.lock();
        try {
            localMonitor = spiderFileMonitor;
            if (localMonitor != null) {
                localMonitor.close();
                spiderFileMonitor = null;
            }
            localTask = spiderPreviewTask;
            if (localTask != null && !localTask.isDone()) {
                localTask.cancel(true);
                spiderPreviewTask = null;
            }
            localSpider = spider;
            if (localSpider != null) {
                localSpider.destroy();
                spider = null;
            }
        } finally {
            spiderLock.unlock();
        }
    }

    private void updateHomeTab() {
        ImageHelper.clearCache();
        Platform.runLater(() -> homeTabLoadingProperty.set(true));
        spiderPreviewTask = spiderPreviewExecutor.submit(() -> {
            JSSpider localSpider;
            Result result;
            AbsSortXml absSortXml;
            List<MovieSort.SortData> sortDataList;
            Movie movieDataInfo;
            List<Movie.Video> movies;

            // 加锁取spider
            spiderLock.lock();
            try {
                localSpider = spider;
                if (localSpider == null) {
                    Platform.runLater(() -> homeTabLoadingProperty.set(false));

                    return;
                }
            } finally {
                spiderLock.unlock();
            }
            // 调用spider
            try {
                result = GsonUtil.fromJson(localSpider.homeContent(false), Result.class);
                log.debug("load homeContent result: {}", result);
            } catch (Exception e) {
                // 爬虫在被调用时销毁，原因可能是用户删除或切换了爬虫，要正确处理这种情况
                if (!tryHandleExecutionInterrupt(e)) {
                    handleException(homeTabLoadingProperty, I18nKeys.SPIDER_DEBUGGING_HOME_INVOKE_ERROR, e);
                }
                Platform.runLater(() -> homeTabLoadingProperty.set(false));

                return;
            }
            if (result == null) {
                Platform.runLater(() -> homeTabLoadingProperty.set(false));

                return;
            }
            absSortXml = beanConverter.resultToAbsSortXml(result, SOURCE_KEY);
            sortDataList = absSortXml.getClasses().getSortList();
            movieDataInfo = absSortXml.getList();
            movies = movieDataInfo.getVideoList();
            Platform.runLater(() -> {
                List<MovieSort.SortData> classItems = classesListView.getItems();
                List<Node> movieListNodes = movieListHBox.getChildren();

                classItems.clear();
                movieListNodes.clear();
                if (!movies.isEmpty()) {
                    for (Movie.Video movie : movies) {
                        ImageView moviePicImageView = new ImageView();
                        String picUrl = movie.getPic();
                        InfoOverlay movieInfoOverlay;

                        moviePicImageView.setImage(BaseResources.PICTURE_PLACEHOLDER_IMG);
                        moviePicImageView.setFitWidth(MOVIE_CELL_WIDTH);
                        moviePicImageView.setFitHeight(MOVIE_CELL_HEIGHT);
                        ImageHelper.loadAsync(picUrl)
                                .thenAccept(imgResult -> {
                                    if (imgResult.isSuccess()) {
                                        Platform.runLater(
                                                () -> moviePicImageView.setImage(imgResult.getImage())
                                        );
                                    }
                                });
                        movieInfoOverlay = new InfoOverlay(moviePicImageView, movie.getName());
                        movieInfoOverlay.getStyleClass().add("movie-info-overlay");
                        movieInfoOverlay.setOnMouseClicked(
                                evt -> selectHomeMovie(evt, movieInfoOverlay, movie)
                        );
                        movieListNodes.add(movieInfoOverlay);
                    }
                }
                if (!sortDataList.isEmpty()) {
                    classItems.addAll(sortDataList);
                }
                homeTabLoadingProperty.set(false);
            });
        });
    }

    /**
     * 如果可以，处理爬虫中断异常。
     * 爬虫中断是预期场景，当用户切换、删除爬虫脚本时，就会发生爬虫中断。
     * @param e 异常
     * @return 是否为爬虫中断异常
     */
    private boolean tryHandleExecutionInterrupt(Exception e) {
        String message = e.getMessage();

        if (
                e instanceof InterruptedException ||
                e instanceof InterruptedIOException ||
                e instanceof PolyglotException && "Context execution was cancelled.".equals(message)
        ) {
            log.debug("user cancelled execution", e);
            Platform.runLater(() -> homeTabLoadingProperty.set(false));

            return true;
        }

        return false;
    }

    private void handleException(BooleanProperty loadingProperty, String messageI18n, @Nullable Exception e) {
        if (e != null) {
            log.error("exception", e);
        }
        Platform.runLater(() -> {
            loadingProperty.set(false);
            ToastHelper.showErrorI18n(messageI18n);
        });
    }

    private void selectHomeMovie(MouseEvent mouseEvent, InfoOverlay node, Movie.Video movie) {
        List<String> styleClasses;

        if (mouseEvent.getButton() != MouseButton.PRIMARY || lastSelectedMovieInfoOverlay == node) {

            return;
        }
        styleClasses = node.getStyleClass();
        styleClasses.remove("movie-info-overlay");
        styleClasses.add("movie-info-overlay-selected");
        if (lastSelectedMovieInfoOverlay != null) {
            styleClasses = lastSelectedMovieInfoOverlay.getStyleClass();
            styleClasses.remove("movie-info-overlay-selected");
            styleClasses.add("movie-info-overlay");
        }
        lastSelectedMovieInfoOverlay = node;
        movieNameLabel.setText(movie.getName());
        movieIdTextField.setText(movie.getId());
        moviePictureUrlTextField.setText(movie.getPic());
    }

    @FXML
    private void onClassesListViewClick(MouseEvent mouseEvent) {
        MovieSort.SortData sortData;
        String sortDataId;
        List<TreeItem<MovieSortFilterTreeNode>> movieSortFilterTreeItems;

        if (mouseEvent.getTarget() instanceof ListCell<?> listCell) {
            sortData = CastUtil.cast(listCell.getItem());
            if (sortData == null || Objects.equals(sortDataId = sortData.getId(), movieClassIdTextField.getText())) {

                return;
            }
            movieClassNameLabel.setText(sortData.getName());
            movieClassIdTextField.setText(sortDataId);
            movieSortFilterCheckTreeView.getCheckModel().clearChecks();
            movieSortFilterTreeItems = movieSortFilterCheckTreeView.getRoot().getChildren();
            movieSortFilterTreeItems.clear();
            for (MovieSort.SortFilter filter : sortData.getFilters()) {
                movieSortFilterTreeItems.add(MovieSortFilterCheckBoxTreeItem.from(filter));
            }
        }
    }

    @FXML
    private void onMovieSortFilterResetButtonAction() {
        movieSortFilterCheckTreeView.getCheckModel().clearChecks();
    }

    @FXML
    private void onOpenLogConsoleButtonAction() {
        router.openSecondary(Views.LOG_CONSOLE_DIALOG, I18nKeys.SETTINGS_DEBUGGING_LOG_CONSOLE);
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

            return;
        }
        spiderSelectComboBox.getProperties().put(SPIDER_SELECT_COMBOBOX_ACTION_FLAG, true);
        initSpider(spiderFile, spiderDebugging);
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
                    clearUI();
                    ToastHelper.showSuccessI18n(I18nKeys.COMMON_MESSAGE_SUCCESS);
                    AsyncUtil.execute(() -> {
                        log.info("delete spider debugging: {}", spiderDebugging);
                        StorageHelper.delete(spiderDebugging);
                        destroySpiderIfExists();
                    });
                }
        );
    }

    private void clearUI() {
        spiderSelectComboBox.getSelectionModel().clearSelection();
        movieSortFilterCheckTreeView.getCheckModel().clearChecks();
        movieListHBox.getChildren().clear();
        movieNameLabel.setText(null);
        movieIdTextField.setText(null);
        moviePictureUrlTextField.setText(null);
        movieClassNameLabel.setText(null);
        movieClassIdTextField.setText(null);
        movieSortFilterCheckTreeView.getRoot().getChildren().clear();
        setSpiderMonitoringStatus(null);
        spiderLoadingProperty.set(false);
        homeTabLoadingProperty.set(false);
    }

    @FXML
    private void onMovieIdCopyButtonAction() {
        String text = movieIdTextField.getText();

        if (text == null) {

            return;
        }
        ClipboardHelper.setContent(text);
        ToastHelper.showMouseToastI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
    }

    @FXML
    private void onMoviePictureUrlCopyButtonAction() {
        String text = moviePictureUrlTextField.getText();

        if (text == null) {

            return;
        }
        ClipboardHelper.setContent(text);
        ToastHelper.showMouseToastI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
    }

    @FXML
    private void onSendToMovieDetailButtonAction() {
        // TODO 发送到影视详情
    }
}
