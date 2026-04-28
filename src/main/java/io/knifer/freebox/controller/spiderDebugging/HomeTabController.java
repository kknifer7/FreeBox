package io.knifer.freebox.controller.spiderDebugging;

import io.knifer.freebox.component.node.MovieSortFilterCheckBoxTreeItem;
import io.knifer.freebox.constant.*;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.controller.SpiderDebuggingController;
import io.knifer.freebox.helper.ClipboardHelper;
import io.knifer.freebox.helper.ImageHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.common.catvod.Result;
import io.knifer.freebox.model.common.tvbox.AbsSortXml;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.model.common.tvbox.MovieSort;
import io.knifer.freebox.model.domain.MovieSortFilterTreeNode;
import io.knifer.freebox.net.websocket.converter.CatVodBeanConverter;
import io.knifer.freebox.spider.js.JSSpider;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.NodeUtil;
import io.knifer.freebox.util.json.GsonUtil;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckTreeView;
import org.controlsfx.control.InfoOverlay;
import org.controlsfx.control.SearchableComboBox;

import java.util.List;
import java.util.Objects;

/**
 * 爬虫调试 - 首页
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public class HomeTabController extends SpiderDebuggingTabController {

    @FXML
    private BorderPane homeTabBorderPane;
    @FXML
    private ProgressIndicator homeTabLoadingProgressIndicator;
    @FXML
    @Getter
    private ListView<MovieSort.SortData> classesListView;
    @FXML
    private HBox movieListHBox;
    @FXML
    private TextField homeMovieClassIdTextField;
    @FXML
    private Label movieClassNameLabel;
    @FXML
    @Getter
    private CheckTreeView<MovieSortFilterTreeNode> homeMovieSortFilterCheckTreeView;
    @FXML
    private Label movieNameLabel;
    @FXML
    private TextField movieIdTextField;
    @FXML
    private TextField moviePictureUrlTextField;

    private InfoOverlay lastSelectedHomeMovieInfoOverlay;

    private BooleanProperty homeTabLoadingProperty;

    private final Context context;
    private final CatVodBeanConverter beanConverter;

    private final static double MOVIE_CELL_WIDTH = 150;
    private final static double MOVIE_CELL_HEIGHT = 200;

    @FXML
    private void initialize() {
        homeTabLoadingProperty = new SimpleBooleanProperty(false);
        context.registerEventListener(
                AppEvents.SpiderDebuggingViewInitialized.class,
                evt -> {
                    SpiderDebuggingController controller = evt.controller();

                    applyParentController(controller);
                    homeTabLoadingProgressIndicator.visibleProperty()
                            .bind(homeTabLoadingProperty.or(spiderLoadingProperty));
                    homeTabBorderPane.visibleProperty()
                            .bind(homeTabLoadingProperty.not().and(spiderLoadingProperty.not()));
                    controller.setHomeTabController(this);
                }
        );
    }

    @Override
    public void clear() {
        homeMovieSortFilterCheckTreeView.getCheckModel().clearChecks();
        homeMovieSortFilterCheckTreeView.getRoot().getChildren().clear();
        movieListHBox.getChildren().clear();
        movieNameLabel.setText(null);
        movieIdTextField.setText(null);
        moviePictureUrlTextField.setText(null);
        classesListView.getItems().clear();
        movieClassNameLabel.setText(null);
        homeMovieClassIdTextField.setText(null);
        lastSelectedHomeMovieInfoOverlay = null;
    }

    @Override
    public void reload() {
        ImageHelper.clearCache();
        homeTabLoadingProperty.set(true);
        spiderPreviewTaskMap.compute(SourceAuditType.HOME, (k, spiderPreviewTask) -> {
            if (spiderPreviewTask != null) {
                parentController.cancelSpiderTaskIfNeeded(spiderPreviewTask);
            }

            return spiderPreviewExecutor.submit(() -> {
                JSSpider localSpider;
                Result result;
                AbsSortXml absSortXml;
                List<MovieSort.SortData> sortDataList;
                Movie movieDataInfo;
                List<Movie.Video> movies;

                localSpider = parentController.requireSpider();
                if (localSpider == null) {
                    Platform.runLater(this::completeReloading);

                    return;
                }
                try {
                    log.debug("loading homeContent, filter=false");
                    result = GsonUtil.fromJson(localSpider.homeContent(false), Result.class);
                    log.debug("load homeContent result: {}", result);
                } catch (Exception e) {
                    // 要排除用户删除、切换爬虫，或者爬虫热更新，导致当前爬虫实例调用中就被销毁的情况，这种属于正常情况
                    if (!parentController.tryHandleExecutionInterrupt(e)) {
                        log.error("homeContent exception", e);
                    }
                    Platform.runLater(this::completeReloading);

                    return;
                }
                if (result == null) {
                    Platform.runLater(this::completeReloading);

                    return;
                }
                absSortXml = beanConverter.resultToAbsSortXml(result, BaseValues.DEBUGGING_SOURCE_KEY);
                sortDataList = absSortXml.getClasses().getSortList();
                movieDataInfo = absSortXml.getList();
                movies = movieDataInfo.getVideoList();
                Platform.runLater(() -> {
                    ObservableList<MovieSort.SortData> classItems = classesListView.getItems();
                    List<Node> movieListNodes = movieListHBox.getChildren();
                    MovieExploreTabController movieExploreTabController =
                            parentController.getMovieExploreTabController();
                    Tab currentTab;
                    SourceAuditType currentTabType;

                    movieExploreTabController.stashParameterForm();
                    movieExploreTabController.clear();
                    classItems.setAll(sortDataList);
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
                            movieInfoOverlay.setUserData(movie);
                            movieListNodes.add(movieInfoOverlay);
                        }
                    }
                    completeReloading();
                    currentTab = previewTabPane.getSelectionModel().getSelectedItem();
                    currentTabType = CastUtil.cast(currentTab.getProperties().get("type"));
                    if (
                            currentTabType == SourceAuditType.MOVIE_EXPLORE &&
                            movieExploreTabController.isAutoRefreshOn()
                    ) {
                        movieExploreTabController.reload();
                    }
                });
            });
        });
    }

    private void completeReloading() {
        context.postEvent(new AppEvents.SpiderDebuggingViewTabLoaded(SourceAuditType.HOME));
        homeTabLoadingProperty.set(false);
    }

    private void selectHomeMovie(MouseEvent mouseEvent, InfoOverlay node, Movie.Video movie) {
        if (mouseEvent.getButton() != MouseButton.PRIMARY || lastSelectedHomeMovieInfoOverlay == node) {

            return;
        }
        NodeUtil.replaceStyleClass(node, "movie-info-overlay", "movie-info-overlay-selected");
        if (lastSelectedHomeMovieInfoOverlay != null) {
            NodeUtil.replaceStyleClass(
                    lastSelectedHomeMovieInfoOverlay,
                    "movie-info-overlay-selected",
                    "movie-info-overlay"
            );
        }
        lastSelectedHomeMovieInfoOverlay = node;
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
            if (sortData == null || Objects.equals(sortDataId = sortData.getId(), homeMovieClassIdTextField.getText())) {

                return;
            }
            movieClassNameLabel.setText(sortData.getName());
            homeMovieClassIdTextField.setText(sortDataId);
            homeMovieSortFilterCheckTreeView.getCheckModel().clearChecks();
            movieSortFilterTreeItems = homeMovieSortFilterCheckTreeView.getRoot().getChildren();
            movieSortFilterTreeItems.clear();
            for (MovieSort.SortFilter filter : sortData.getFilters()) {
                movieSortFilterTreeItems.add(MovieSortFilterCheckBoxTreeItem.from(filter));
            }
        }
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
    private void onSendToMovieExploreButtonAction() {
        MovieExploreTabController movieExploreTabController = parentController.getMovieExploreTabController();
        CheckTreeView<MovieSortFilterTreeNode> movieExploreSortFilterCheckTreeView =
                movieExploreTabController.getMovieExploreSortFilterCheckTreeView();
        SearchableComboBox<MovieSort.SortData> movieExploreClassComboBox =
                movieExploreTabController.getMovieExploreClassComboBox();
        TextField movieExplorePageNoTextField = movieExploreTabController.getMovieExplorePageNoTextField();
        String sortId = homeMovieClassIdTextField.getText();

        if (StringUtils.isEmpty(sortId)) {
            ToastHelper.showWarningI18n(I18nKeys.SPIDER_DEBUGGING_COMMON_SORT_ID_REQUIRED);

            return;
        }
        CollectionUtil.findFirst(
                movieExploreClassComboBox.getItems(), sortData -> Objects.equals(sortData.getId(), sortId)
        ).ifPresent(movieExploreClassComboBox::setValue);
        parentController.copyMovieSortFilterCheckTreeViewChecks(
                homeMovieSortFilterCheckTreeView, movieExploreSortFilterCheckTreeView
        );
        movieExplorePageNoTextField.setText("1");
        previewTabPane.getSelectionModel().select(MOVIE_EXPLORE_TAB_IDX);
        movieExploreTabController.reload();
    }

    @FXML
    private void onMovieSortFilterResetButtonAction() {
        homeMovieSortFilterCheckTreeView.getCheckModel().clearChecks();
    }

    @FXML
    private void onSendToMovieDetailButtonAction() {
        parentController.sendToMovieDetailTab(movieIdTextField.getText());
    }

    @Override
    public BooleanProperty getLoadingProperty() {
        return homeTabLoadingProperty;
    }

    @Override
    public boolean isAutoRefreshOn() {
        return true;
    }
}