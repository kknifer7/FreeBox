package io.knifer.freebox.controller.spiderDebugging;

import io.knifer.freebox.component.factory.VideoGridCellFactory;
import io.knifer.freebox.component.node.MovieSortFilterCheckBoxTreeItem;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.controller.SpiderDebuggingController;
import io.knifer.freebox.helper.ClipboardHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.common.catvod.Result;
import io.knifer.freebox.model.common.tvbox.AbsXml;
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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckTreeView;
import org.controlsfx.control.GridView;
import org.controlsfx.control.InfoOverlay;
import org.controlsfx.control.SearchableComboBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 爬虫调试 - 影视浏览
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public class MovieExploreTabController extends SpiderDebuggingTabController {

    @FXML
    private CheckBox movieExploreAutoRefreshCheckBox;
    @FXML
    private BorderPane movieExploreTabBorderPane;
    @FXML
    private ProgressIndicator movieExploreTabLoadingProgressIndicator;
    @FXML
    private ProgressIndicator movieExploreListLoadingProgressIndicator;
    @FXML
    private GridView<Movie.Video> movieExploreGridView;
    @FXML
    @Getter
    private SearchableComboBox<MovieSort.SortData> movieExploreClassComboBox;
    @FXML
    @Getter
    private CheckTreeView<MovieSortFilterTreeNode> movieExploreSortFilterCheckTreeView;
    @FXML
    @Getter
    private TextField movieExplorePageNoTextField;
    @FXML
    private TextField pageCountTextField;
    @FXML
    private TextField totalTextField;
    @FXML
    private TextField limitTextField;
    @FXML
    public Label hasNextLabel;
    @FXML
    private TextField movieIdTextField;
    @FXML
    private TextField moviePictureUrlTextField;

    private BooleanProperty movieExploreLoadingProperty;
    private BooleanProperty movieExploreListLoadingProperty;

    private VideoGridCellFactory videoGridCellFactory;

    private InfoOverlay lastSelectedExploreMovieInfoOverlay;
    private ParameterForm stashedParameterForm;

    private final Context context;
    private final CatVodBeanConverter beanConverter;

    @FXML
    private void initialize() {
        movieExploreLoadingProperty = new SimpleBooleanProperty(false);
        movieExploreListLoadingProperty = new SimpleBooleanProperty(false);
        videoGridCellFactory = new VideoGridCellFactory(this::selectExploreMovie, cell -> {});

        movieExploreGridView.setCellFactory(videoGridCellFactory);
        movieExploreClassComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(MovieSort.SortData sortData) {
                if (sortData == null) {

                    return null;
                }

                return String.format("%s (%s)", sortData.getId(), sortData.getName());
            }

            @Override
            public MovieSort.SortData fromString(String sortId) {
                if (sortId == null) {

                    return null;
                }

                return CollectionUtil.findFirst(
                        movieExploreClassComboBox.getItems(),
                        sortData -> sortId.equals(sortData.getId())
                ).orElse(null);
            }
        });
        context.registerEventListener(
                AppEvents.SpiderDebuggingViewInitialized.class,
                evt -> {
                    SpiderDebuggingController controller = evt.controller();
                    HomeTabController homeTabController;
                    ListView<MovieSort.SortData> classesListView;
                    BooleanProperty homeTabLoadingProperty;

                    applyParentController(controller);
                    homeTabController = parentController.getHomeTabController();
                    classesListView = homeTabController.getClassesListView();
                    homeTabLoadingProperty = homeTabController.getLoadingProperty();
                    movieExploreClassComboBox.itemsProperty().bind(classesListView.itemsProperty());
                    movieExploreTabLoadingProgressIndicator.visibleProperty()
                            .bind(spiderLoadingProperty.or(homeTabLoadingProperty));
                    movieExploreListLoadingProgressIndicator.visibleProperty()
                            .bind(movieExploreListLoadingProperty.and(
                                    movieExploreTabLoadingProgressIndicator.visibleProperty().not()
                            ));
                    movieExploreTabBorderPane.visibleProperty()
                            .bind(homeTabLoadingProperty.not().and(spiderLoadingProperty.not()));
                    parentController.setMovieExploreTabController(this);
                }
        );
        stashedParameterForm = new ParameterForm();
        Platform.runLater(() -> {
            Stage stage = WindowHelper.getStage(movieExploreAutoRefreshCheckBox);

            stage.setOnHidden(evt -> videoGridCellFactory.destroy());
        });
    }

    private void selectExploreMovie(Movie.Video movie) {
        String movieId = movie.getId();
        InfoOverlay node = videoGridCellFactory.getMovieInfoOverlayByItemId(movieId);

        if (node == null) {

            return;
        }
        NodeUtil.replaceStyleClass(node, "movie-info-overlay", "movie-info-overlay-selected");
        if (lastSelectedExploreMovieInfoOverlay != null) {
            NodeUtil.replaceStyleClass(
                    lastSelectedExploreMovieInfoOverlay,
                    "movie-info-overlay-selected",
                    "movie-info-overlay"
            );
        }
        lastSelectedExploreMovieInfoOverlay = node;
        movieIdTextField.setText(movieId);
        moviePictureUrlTextField.setText(movie.getPic());
    }

    @Override
    public void clear() {
        movieExploreClassComboBox.getSelectionModel().clearSelection();
        movieExplorePageNoTextField.setText(null);
        movieExploreSortFilterCheckTreeView.getCheckModel().clearChecks();
        movieExploreSortFilterCheckTreeView.getRoot().getChildren().clear();
        movieExploreSortFilterCheckTreeView.getProperties().remove("sortId");
        pageCountTextField.setText(null);
        totalTextField.setText(null);
        limitTextField.setText(null);
        hasNextLabel.setText(null);
        movieIdTextField.setText(null);
        moviePictureUrlTextField.setText(null);
        movieExploreGridView.getItems().clear();
        movieExploreLoadingProperty.set(false);
        movieExploreListLoadingProperty.set(false);
    }

    @FXML
    private void onSendToMovieDetailButtonAction() {
        parentController.sendToMovieDetailTab(movieIdTextField.getText());
    }

    @FXML
    private void onMovieExploreClassComboBoxAction() {
        MovieSort.SortData sortData = movieExploreClassComboBox.getValue();
        List<TreeItem<MovieSortFilterTreeNode>> treeItems;
        Map<Object, Object> treeViewProperties;

        if (sortData == null) {

            return;
        }
        treeViewProperties = movieExploreSortFilterCheckTreeView.getProperties();
        if (Objects.equals(treeViewProperties.get("sortId"), sortData.getId())) {

            return;
        }
        movieExploreSortFilterCheckTreeView.getCheckModel().clearChecks();
        treeItems = movieExploreSortFilterCheckTreeView.getRoot().getChildren();
        treeItems.clear();
        for (MovieSort.SortFilter filter : sortData.getFilters()) {
            treeItems.add(MovieSortFilterCheckBoxTreeItem.from(filter));
        }
        treeViewProperties.put("sortId", sortData.getId());
    }

    @FXML
    private void onMovieExploreSearchButtonAction() {
        reload();
    }

    @Override
    public void reload() {
        MovieSort.SortData sortData = movieExploreClassComboBox.getValue();
        String sortId;
        boolean useStashedParameterForm = false;

        if (sortData == null) {
            sortId = stashedParameterForm.getSortId();
            if (sortId == null) {
                completeReloading();

                return;
            }
            for (MovieSort.SortData sd : movieExploreClassComboBox.getItems()) {
                if (sortId.equals(sd.getId())) {
                    useStashedParameterForm = true;
                    sortData = sd;
                    break;
                }
            }
            if (!useStashedParameterForm) {
                completeReloading();

                return;
            }
        }
        if (movieExplorePageNoTextField.getText() == null) {
            movieExplorePageNoTextField.setText("1");
        }
        if (useStashedParameterForm) {
            movieExploreClassComboBox.getSelectionModel().select(sortData);
            movieExplorePageNoTextField.setText(stashedParameterForm.getPageNo());
            applyStashedFilters();
            stashedParameterForm.clear();
        }
        movieExploreGridView.getItems().clear();
        pageCountTextField.setText(null);
        totalTextField.setText(null);
        limitTextField.setText(null);
        hasNextLabel.setText(null);
        movieIdTextField.setText(null);
        moviePictureUrlTextField.setText(null);
        doMovieExploreSearch();
    }

    private void completeReloading() {
        context.postEvent(new AppEvents.SpiderDebuggingViewTabLoaded(SourceAuditType.MOVIE_EXPLORE));
        movieExploreListLoadingProperty.set(false);
    }

    private void doMovieExploreSearch() {
        String sortId = movieExploreClassComboBox.getValue().getId();
        String pageNo = movieExplorePageNoTextField.getText();
        HashMap<String, String> filters = getCategoryContentFiltersParam();

        movieExploreListLoadingProperty.set(true);
        spiderPreviewTaskMap.compute(SourceAuditType.MOVIE_EXPLORE, (k, spiderPreviewTask) -> {
            if (spiderPreviewTask != null) {
                parentController.cancelSpiderTaskIfNeeded(spiderPreviewTask);
            }

            return spiderPreviewExecutor.submit(() -> {
                JSSpider localSpider;
                Result result;
                AbsXml categoryContent;
                Movie movieInfo;
                List<Movie.Video> movies;

                localSpider = parentController.requireSpider();
                if (localSpider == null) {
                    Platform.runLater(this::completeReloading);

                    return;
                }
                try {
                    log.debug("categoryContent params, sortId={}, pageNo={}, filters={}", sortId, pageNo, filters);
                    result = GsonUtil.fromJson(
                            localSpider.categoryContent(sortId, pageNo, !filters.isEmpty(), filters),
                            Result.class
                    );
                    log.debug("load categoryContent result: {}", result);
                } catch (Exception e) {
                    if (!parentController.tryHandleExecutionInterrupt(e)) {
                        log.error("result exception", e);
                        Platform.runLater(this::completeReloading);
                    }

                    return;
                }
                if (result == null) {
                    Platform.runLater(this::completeReloading);

                    return;
                }
                categoryContent = beanConverter.resultToAbsXml(result, BaseValues.DEBUGGING_SOURCE_KEY);
                movieInfo = categoryContent.getMovie();
                movies = movieInfo.getVideoList();
                if (movies.isEmpty()) {
                    Platform.runLater(this::completeReloading);

                    return;
                }
                Platform.runLater(() -> {
                    ObservableList<Movie.Video> items = movieExploreGridView.getItems();
                    int pageCount = movieInfo.getPagecount();
                    int recordCount = movieInfo.getRecordcount();
                    int pageSize = movieInfo.getPagesize();
                    boolean hasNext =
                            pageCount > movieInfo.getPage() || (pageSize > 0 && recordCount > pageSize * pageCount);
                    String hasNextStr;

                    items.setAll(movies);
                    pageCountTextField.setText(String.valueOf(pageCount));
                    totalTextField.setText(String.valueOf(recordCount));
                    limitTextField.setText(String.valueOf(pageSize));
                    if (hasNext) {
                        hasNextStr = I18nHelper.get(I18nKeys.COMMON_YES);
                        NodeUtil.replaceStyleClass(hasNextLabel, "danger", "success");
                    } else {
                        hasNextStr = I18nHelper.get(I18nKeys.COMMON_NO);
                        NodeUtil.replaceStyleClass(hasNextLabel, "success", "danger");
                    }
                    hasNextLabel.setText(hasNextStr);
                    completeReloading();
                });
            });
        });
    }

    @FXML
    private void onMovieSortFilterResetButtonAction() {
        movieExploreSortFilterCheckTreeView.getCheckModel().clearChecks();
    }

    @Override
    public BooleanProperty getLoadingProperty() {
        return movieExploreLoadingProperty;
    }

    @Override
    public boolean isAutoRefreshOn() {
        return movieExploreAutoRefreshCheckBox.isSelected();
    }

    /**
     * 暂存参数表单
     * 会在爬虫文件更新时被调用，用于在需要自动刷新时作为参数
     */
    public void stashParameterForm() {
        MovieSort.SortData sortData = movieExploreClassComboBox.getValue();

        stashedParameterForm.setSortId(sortData == null ? null : sortData.getId());
        stashedParameterForm.setPageNo(movieExplorePageNoTextField.getText());
        stashedParameterForm.setFilters(getCategoryContentFiltersParam());
    }

    private HashMap<String, String> getCategoryContentFiltersParam() {
        return (HashMap<String, String>) movieExploreSortFilterCheckTreeView.getCheckModel().getCheckedItems().stream()
                .map(TreeItem::getValue)
                .collect(Collectors.toMap(
                        MovieSortFilterTreeNode::getFilterKey, MovieSortFilterTreeNode::getFilterValue
                ));
    }

    /**
     * 将暂存的筛选条件恢复到当前筛选树中
     */
    private void applyStashedFilters() {
        HashMap<String, String> filters = stashedParameterForm.getFilters();
        ObservableList<TreeItem<MovieSortFilterTreeNode>> targetTreeParentItems;
        String filterKey;
        String filterValue;

        if (
                filters.isEmpty() ||
                (targetTreeParentItems = movieExploreSortFilterCheckTreeView.getRoot().getChildren()).isEmpty()
        ) {

            return;
        }
        for (TreeItem<MovieSortFilterTreeNode> targetFilterParentItem : targetTreeParentItems) {
            filterKey = targetFilterParentItem.getValue().getFilterKey();
            filterValue = filters.get(filterKey);
            if (filterValue == null) {
                continue;
            }
            for (TreeItem<MovieSortFilterTreeNode> targetFilterItem : targetFilterParentItem.getChildren()) {
                if (Objects.equals(filterValue, targetFilterItem.getValue().getFilterValue())) {
                    movieExploreSortFilterCheckTreeView.getCheckModel().check(targetFilterItem);
                    break;
                }
            }
        }
    }

    @FXML
    private void onCopyButtonAction(ActionEvent actionEvent) {
        Button copyButton = CastUtil.cast(actionEvent.getSource());
        TextField textField = (TextField) copyButton.getUserData();
        String text = textField.getText();

        if (StringUtils.isEmpty(text)) {

            return;
        }
        ClipboardHelper.setContent(text);
        ToastHelper.showMouseToastI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
    }

    @Data
    private static class ParameterForm {
        private String sortId;
        private String pageNo;
        private HashMap<String, String> filters;

        public void clear() {
            sortId = null;
            pageNo = null;
            filters = null;
        }

        public HashMap<String, String> getFilters() {
            return filters == null ? new HashMap<>() : filters;
        }
    }
}
