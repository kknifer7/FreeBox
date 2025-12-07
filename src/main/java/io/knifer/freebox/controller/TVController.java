package io.knifer.freebox.controller;

import io.knifer.freebox.component.factory.VideoGridCellFactory;
import io.knifer.freebox.component.node.MovieInfoListPopOver;
import io.knifer.freebox.component.node.MovieRankPopOver;
import io.knifer.freebox.component.node.MovieSortFilterPopOver;
import io.knifer.freebox.component.node.SourceBeanBlockPopOver;
import io.knifer.freebox.component.node.player.BasePlayer;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.Views;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.handler.MovieSuggestionHandler;
import io.knifer.freebox.handler.impl.IQiYiMovieSuggestionHandler;
import io.knifer.freebox.handler.impl.Kan360MovieFetchingHandler;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.bo.VideoDetailsBO;
import io.knifer.freebox.model.bo.VideoPlayInfoBO;
import io.knifer.freebox.model.common.tvbox.*;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.domain.ClientTVProperties;
import io.knifer.freebox.model.s2c.*;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.service.MovieSearchService;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.FXMLUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.GridView;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 客户端电视
 *
 * @author Knifer
 */
@Slf4j
public class TVController implements Destroyable {

    @FXML
    private BorderPane root;
    @FXML
    private ComboBox<SourceBean> sourceBeanComboBox;
    @FXML
    private ListView<MovieSort.SortData> classesListView;
    @FXML
    private GridView<Movie.Video> videosGridView;
    @FXML
    private ProgressIndicator sortsLoadingProgressIndicator;
    @FXML
    private ProgressIndicator movieLoadingProgressIndicator;
    @FXML
    private Button sourceBeanBlockButton;
    @FXML
    private Button historyButton;
    @FXML
    private Button collectButton;
    @FXML
    private Button classFilterButton;
    @FXML
    private Button searchButton;
    @FXML
    private TextField searchTextField;
    @FXML
    private ProgressIndicator searchLoadingProgressIndicator;

    private MovieSearchService movieSearchService;

    private Stage stage;
    private SourceBeanBlockPopOver sourceBeanBlockPopOver;
    private MovieInfoListPopOver movieHistoryPopOver;
    private MovieInfoListPopOver movieCollectionPopOver;
    private MovieSortFilterPopOver movieSortFilterPopOver;
    private MovieRankPopOver movieRankPopOver;

    private AutoCompletionBinding<String> searchTextFieldAutoCompletionBinding;

    private final BooleanProperty sortsLoadingProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty movieLoadingProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty searchLoadingProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty classFilterButtonDisableProperty = new SimpleBooleanProperty(true);
    private SpiderTemplate template;
    private ClientManager clientManager;
    private Movie.Video fetchMoreItem;
    private ClientTVProperties clientTVPropertiesBackup;
    private ClientTVProperties clientTVProperties;

    private final MovieSuggestionHandler movieSuggestionHandler = new IQiYiMovieSuggestionHandler();

    private final String HOME_SORT_DATA_ID = "ztx*1RcW6%Ep";

    private final Map<String, MutablePair<Movie, List<Movie.Video>>> MOVIE_CACHE = new HashMap<>();

    @FXML
    private void initialize() {
        template = Context.INSTANCE.getSpiderTemplate();
        fetchMoreItem = new Movie.Video();
        fetchMoreItem.setId(BaseValues.LOAD_MORE_ITEM_ID);
        fetchMoreItem.setName(I18nHelper.get(I18nKeys.TV_LOAD_MORE));
        Context.INSTANCE.registerEventListener(
                AppEvents.ClientUnregisteredEvent.class,
                evt -> LoadingHelper.hideLoading()
        );
        Platform.runLater(() -> {
            clientManager = Context.INSTANCE.getClientManager();
            stage = WindowHelper.getStage(root);
            stage.setOnCloseRequest(evt -> {
               destroy();
               Context.INSTANCE.popAndShowLastStage();
            });
            movieSearchService = new MovieSearchService(keywordAndSearchContent ->
                Platform.runLater(() -> {
                    String keyword = keywordAndSearchContent.getLeft();
                    AbsXml searchContent;

                    if (
                            movieSearchService.getState() != Service.State.SUCCEEDED ||
                                    !keyword.equals(movieSearchService.getKeyword())
                    ) {
                        // 任务已取消

                        return;
                    }
                    searchContent = keywordAndSearchContent.getRight();
                    putVideosInView(
                            MutablePair.of(searchContent.getMovie(), searchContent.getMovie().getVideoList()),
                            false
                    );
                }), () -> searchLoadingProperty.set(false));

            // 源屏蔽弹出框
            sourceBeanBlockPopOver = new SourceBeanBlockPopOver(sourceBeans -> {
                clearMovieData();
                videosGridView.getItems().clear();
                updateSourceBeanData(sourceBeans);
            });
            // 历史记录弹出框
            movieHistoryPopOver = new MovieInfoListPopOver(
                    I18nKeys.TV_HISTORY,
                    this::onVodAction,
                    vodInfoDeleting -> {
                        movieHistoryPopOver.clearVodInfoList();
                        template.deletePlayHistory(
                                DeletePlayHistoryDTO.of(vodInfoDeleting), this::reloadMovieHistoryPopOver
                        );
                    }
            );
            // 收藏弹出框
            movieCollectionPopOver = new MovieInfoListPopOver(
                    I18nKeys.TV_COLLECTION,
                    this::onVodAction,
                    vodInfoDeleting -> {
                        movieCollectionPopOver.clearVodInfoList();
                        template.deleteMovieCollection(
                                DeleteMovieCollectionDTO.of(vodInfoDeleting), this::reloadMovieCollectionPopOver
                        );
                    }
            );
            // 影片过滤条件弹出框
            movieSortFilterPopOver = new MovieSortFilterPopOver(sortData -> {
                // 影片过滤条件更新，应用最新过滤条件，重新加载影片列表
                MOVIE_CACHE.remove(sortData.getId());
                loadMovieBySortData(sortData);
            });
            // 影片搜索相关
            searchTextFieldAutoCompletionBinding =
                    TextFields.bindAutoCompletion(searchTextField, movieSuggestionHandler::handle);
            movieRankPopOver = new MovieRankPopOver(
                    new Kan360MovieFetchingHandler(),
                    movieTitle -> {
                        // 为了不让搜索自动提示干扰用户，先移除它的绑定
                        searchTextFieldAutoCompletionBinding.dispose();
                        searchTextField.setText(movieTitle);
                        searchTextField.positionCaret(searchTextField.getLength());
                        onSearchBtnAction();
                        // 重新绑定搜索自动提示
                        searchTextFieldAutoCompletionBinding = TextFields.bindAutoCompletion(
                                searchTextField, movieSuggestionHandler::handle
                        );
                    }
            );
            // 影片列表单元格工厂
            videosGridView.setCellFactory(new VideoGridCellFactory(
                    video -> openVideo(video.getSourceKey(), video.getId(), video.getName()),
                    this::loadMoreMovie
            ));
            // 搜索框获取焦点时，显示热搜
            searchTextField.setOnMouseClicked(evt -> {
                if (evt.getButton() != MouseButton.PRIMARY) {

                    return;
                }
                if (movieRankPopOver.isShowing()) {
                    movieRankPopOver.hide();
                } else if (
                        !searchLoadingProperty.get() &&
                        !sortsLoadingProperty.get() &&
                        !movieLoadingProperty.get()
                ) {
                    movieRankPopOver.show(searchTextField);
                }
            });
            movieRankPopOver.show(searchTextField);
            movieRankPopOver.hide();
            // 状态联动绑定
            sourceBeanBlockButton.disableProperty().bind(
                    sourceBeanBlockPopOver.showingProperty().or(sortsLoadingProperty)
            );
            historyButton.disableProperty().bind(movieHistoryPopOver.showingProperty().or(sortsLoadingProperty));
            movieHistoryPopOver.loadingPropertyProperty().bind(movieLoadingProperty);
            collectButton.disableProperty().bind(movieCollectionPopOver.showingProperty().or(sortsLoadingProperty));
            movieCollectionPopOver.loadingPropertyProperty().bind(movieLoadingProperty);
            classFilterButton.disableProperty().bind(
                    movieSortFilterPopOver.showingProperty()
                            .or(sortsLoadingProperty)
                            .or(classFilterButtonDisableProperty)
            );
            sortsLoadingProgressIndicator.visibleProperty().bind(sortsLoadingProperty.or(movieLoadingProperty));
            movieLoadingProgressIndicator.visibleProperty().bind(movieLoadingProperty);
            videosGridView.disableProperty().bind(movieLoadingProperty);
            classesListView.disableProperty().bind(sortsLoadingProperty.or(movieLoadingProperty));
            sourceBeanComboBox.disableProperty().bind(sortsLoadingProperty);
            searchButton.disableProperty().bind(movieLoadingProperty);
            searchLoadingProgressIndicator.visibleProperty().bind(searchLoadingProperty);

            template.init(success -> {
                if (!success) {
                    return;
                }
                clientManager.getCurrentClient()
                        .thenAccept(clientInfo -> {
                            if (clientInfo == null) {

                                return;
                            }
                            initTVByClientInfo(clientInfo);
                        });
            });
        });
    }

    private void reloadMovieHistoryPopOver() {
        if (!movieHistoryPopOver.isShowing()) {
            return;
        }
        template.getPlayHistory(GetPlayHistoryDTO.of(100), playHistory ->
            Platform.runLater(() -> {
                if (CollectionUtil.isNotEmpty(playHistory)) {
                    movieHistoryPopOver.setVodInfoList(playHistory);
                }
                ToastHelper.showSuccessI18n(I18nKeys.COMMON_MESSAGE_SUCCESS);
            })
        );
    }

    private void reloadMovieCollectionPopOver() {
        if (!movieCollectionPopOver.isShowing()) {
            return;
        }
        template.getMovieCollection(movieCollection -> Platform.runLater(() -> {
            if (CollectionUtil.isNotEmpty(movieCollection)) {
                movieCollectionPopOver.setVodInfoList(
                        movieCollection.stream().map(VodInfo::from).toList()
                );
            }
            ToastHelper.showSuccessI18n(I18nKeys.COMMON_MESSAGE_SUCCESS);
        }));
    }

    /**
     * 影片列表项点击事件（播放历史、收藏）
     * @param vod 影片对象
     */
    @FXML
    private void onVodAction(VodInfo vod) {
        String sourceKey;
        String videoId;

        sourceKey = vod.getSourceKey();
        videoId = vod.getId();
        if (videoId == null) {

            return;
        }
        if (vod.getPlayFlag() == null) {
            // 收藏夹中的影片，是不带播放历史记录的，所以先尝试获取一下历史记录信息，携带历史信息打开
            movieCollectionPopOver.hide();
            template.getOnePlayHistory(
                    GetOnePlayHistoryDTO.of(sourceKey, videoId),
                    vodInfo -> openVideo(
                            sourceKey,
                            videoId,
                            vod.getName(),
                            vodInfo == null ? null : VideoPlayInfoBO.of(vodInfo)
                    )
            );
        } else {
            movieHistoryPopOver.hide();
            openVideo(sourceKey, videoId, vod.getName(), VideoPlayInfoBO.of(vod));
        }
    }

    /**
     * 加载更多影片
     * @param loadMoreCell 视图中的“加载更多”项
     */
    private void loadMoreMovie(VideoGridCellFactory.VideoGridCell loadMoreCell) {
        MovieSort.SortData sortData;
        MutablePair<Movie, List<Movie.Video>> movieAndVideoCached;
        Movie movieCached;

        loadMoreCell.setDisable(true);
        sortData = classesListView.getSelectionModel().getSelectedItem();
        if (sortData == null) {
            loadMoreCell.setDisable(false);
            return;
        }
        movieAndVideoCached = MOVIE_CACHE.get(sortData.getId());
        if (movieAndVideoCached == null) {
            loadMoreCell.setDisable(false);
            return;
        }
        movieCached = movieAndVideoCached.getLeft();
        template.getCategoryContent(
                GetCategoryContentDTO.of(getSourceBean(), sortData, movieCached.getPage() + 1),
                categoryContent -> {
                    Movie movie = categoryContent.getMovie();
                    ObservableList<Movie.Video> items = videosGridView.getItems();
                    List<Movie.Video> videos = movie.getVideoList();
                    int loadMoreItemIdx;

                    if (videos.isEmpty()) {
                        return;
                    }
                    loadMoreItemIdx = items.size() - 1;
                    Platform.runLater(() -> {
                        items.addAll(videos);
                        if (movie.getPage() >= movie.getPagecount() || items.size() >= movie.getRecordcount()) {
                            // 没有更多的项了，移除“获取更多”项
                            items.remove(loadMoreItemIdx);
                        } else {
                            // 将“获取更多”项移动到最后
                            Collections.swap(items, loadMoreItemIdx, items.size() - 1);
                        }
                        movieAndVideoCached.setLeft(movie);
                        movieAndVideoCached.setRight(new ArrayList<>(items));
                        loadMoreCell.setDisable(false);
                    });
                }
        );
    }

    /**
     * 打开影片
     * @param sourceKey 源ID
     * @param videoId 影片ID
     * @param videoName 影片名称
     */
    private void openVideo(String sourceKey, String videoId, String videoName) {
        openVideo(sourceKey, videoId, videoName, null);
    }


    /**
     * 打开影片
     * @param sourceKey 源ID
     * @param videoId 影片ID
     * @param videoName 影片名称
     * @param playInfo 播放信息
     */
    private void openVideo(String sourceKey, String videoId, String videoName, @Nullable VideoPlayInfoBO playInfo) {
        SourceBean sourceBean = getSourceBean(sourceKey);

        Platform.runLater(
                () -> LoadingHelper.showLoading(WindowHelper.getStage(root))
        );
        template.getDetailContent(
                GetDetailContentDTO.of(sourceBean.getKey(), videoId),
                detailContent ->
                    Platform.runLater(() -> {
                        Pair<Stage, VideoController> stageAndController;
                        Stage tvStage;
                        Stage videoStage;
                        Movie movie;
                        List<Movie.Video> videos;
                        Movie.Video.UrlBean urlBean;

                        if (
                                detailContent == null ||
                                (movie = detailContent.getMovie()) == null ||
                                CollectionUtil.isEmpty(videos = movie.getVideoList())
                        ) {
                            ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_MOVIE_DETAIL_FAILED);
                            LoadingHelper.hideLoading();

                            return;
                        }
                        urlBean = videos.get(0).getUrlBean();
                        if (CollectionUtil.isEmpty(urlBean.getInfoList())) {
                            // 没有播放列表，有可能不是影片，而是源作者自行添加的广告一类的东西
                            LoadingHelper.hideLoading();

                            return;
                        }
                        stageAndController = FXMLUtil.load(Views.VIDEO);
                        tvStage = WindowHelper.getStage(root);
                        videoStage = stageAndController.getLeft();
                        videoStage.setTitle(videoName);
                        stageAndController.getRight().setData(VideoDetailsBO.of(
                                detailContent,
                                playInfo,
                                sourceBean,
                                BasePlayer.createPlayer((Pane) videoStage.getScene().getRoot()),
                                template,
                                newPlayInfo -> {
                                    if (newPlayInfo != null) {
                                        savePlayHistory(detailContent, newPlayInfo);
                                    }
                                }
                        ));
                        LoadingHelper.hideLoading();
                        WindowHelper.route(tvStage, videoStage);
                    })
        );
    }

    /**
     * 保存播放历史
     * @param detailContent 影片详情
     * @param playInfo 播放信息
     */
    private void savePlayHistory(AbsXml detailContent, VideoPlayInfoBO playInfo) {
        VodInfo vodInfo = new VodInfo();
        Movie.Video video = detailContent.getMovie().getVideoList().get(0);

        vodInfo.setVideo(video);
        vodInfo.setPlayFlag(playInfo.getPlayFlag());
        vodInfo.setPlayIndex(playInfo.getPlayIndex());
        vodInfo.setProgress(playInfo.getProgress());
        vodInfo.setReverseSort(playInfo.isReverseSort());
        vodInfo.setPlayNote(playInfo.getPlayNote());
        template.savePlayHistory(
                SavePlayHistoryDTO.of(vodInfo), exception -> {
                    ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_SAVE_HISTORY_FAILED);
                    ToastHelper.showException(exception);
                }
        );
    }

    private void initTVByClientInfo(ClientInfo clientInfo) {
        Platform.runLater(() -> {
            stage.setTitle(clientInfo.getName());
            initProperties(clientInfo);
            initSourceBeanData();
        });
    }

    private void initProperties(ClientInfo clientInfo) {
        String clientId = clientInfo.getId();

        clientTVProperties = StorageHelper.find(
                clientId, ClientTVProperties.class
        ).orElseGet(() -> ClientTVProperties.from(clientId));
        clientTVPropertiesBackup = clientTVProperties.copy();
    }

    /**
     * 初始化源列表数据
     */
    private void initSourceBeanData() {
        template.getSourceBeanList(sourceBeans -> {
            List<SourceBean> activeSourceBeans;

            if (sourceBeans == null) {
                ToastHelper.showErrorI18n(
                        I18nKeys.TV_ERROR_LOAD_SOURCE_BEAN_LIST_FAILED
                );

                return;
            }
            if (sourceBeans.isEmpty()) {
                ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_SOURCE_BEAN_LIST_EMPTY);

                return;
            }
            activeSourceBeans = sourceBeanBlockPopOver.setSourceBeans(sourceBeans);
            if (!activeSourceBeans.isEmpty()) {
                updateSourceBeanData(activeSourceBeans);
            }
        });
    }

    /**
     * 更新源列表
     * @param sourceBeanList 源对象列表
     */
    private void updateSourceBeanData(List<SourceBean> sourceBeanList) {
        List<SourceBean> items = sourceBeanComboBox.getItems();
        VideoGridCellFactory videoGridCellFactory;
        SingleSelectionModel<SourceBean> selectionModel;
        String sourceKeyLastUsed;

        items.clear();
        if (sourceBeanList.isEmpty()) {
            if (sortsLoadingProperty.get()) {
                sortsLoadingProperty.set(false);
            }

            return;
        }
        items.addAll(sourceBeanList);
        videoGridCellFactory = (VideoGridCellFactory) videosGridView.getCellFactory();
        videoGridCellFactory.setSourceBeans(sourceBeanList);
        movieHistoryPopOver.setSourceBeans(sourceBeanList);
        movieCollectionPopOver.setSourceBeans(sourceBeanList);
        selectionModel = sourceBeanComboBox.getSelectionModel();
        if (selectionModel.getSelectedItem() == null) {
            sourceKeyLastUsed = clientTVProperties.getSourceKeyLastUsed();
            if (StringUtils.isBlank(sourceKeyLastUsed)) {
                selectionModel.selectFirst();
            } else {
                CollectionUtil.findFirst(items, i -> sourceKeyLastUsed.equals(i.getKey()))
                                .ifPresentOrElse(selectionModel::select, selectionModel::selectFirst);
            }
        }
    }

    /**
     * 源选择事件
     */
    @FXML
    private void onSourceBeanComboBoxAction() {
        SourceBean sourceBean = getSourceBean();
        ObservableList<MovieSort.SortData> items;

        if (sourceBean == null) {
            return;
        }
        items = classesListView.getItems();
        sortsLoadingProperty.set(true);
        clearMovieData();
        videosGridView.getItems().clear();
        template.getHomeContent(sourceBean, homeContent ->
            Platform.runLater(() -> {
                Movie movie;
                List<Movie.Video> list;
                MovieSort classes;
                List<MovieSort.SortData> sortList;
                MovieSort.SortData defaultSortData;

                items.clear();
                if (homeContent == null) {
                    Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_SOURCE_FAILED));
                    sortsLoadingProperty.set(false);

                    return;
                }
                movie = homeContent.getList();
                classes = homeContent.getClasses();
                sortList = classes.getSortList();
                if (movie != null && CollectionUtil.isNotEmpty(list = movie.getVideoList())) {
                    // 该源带有首页推荐影片，新增一个首页推荐类别，并且将影片数据缓存起来
                    items.add(new MovieSort.SortData(HOME_SORT_DATA_ID, I18nHelper.get(I18nKeys.TV_HOME)));
                    MOVIE_CACHE.put(HOME_SORT_DATA_ID, MutablePair.of(movie, list));
                }
                items.addAll(sortList);
                sortsLoadingProperty.set(false);
                if (!items.isEmpty()) {
                    classesListView.getSelectionModel().selectFirst();
                    defaultSortData = items.get(0);
                    loadMovieBySortData(defaultSortData);
                    putSortDataFilterListInMovieSortFilterPopOver(defaultSortData);
                }
            })
        );
    }

    /**
     * 类别列表点击事件
     * @param mouseEvent 点击事件
     */
    @FXML
    private void onClassesListViewClick(MouseEvent mouseEvent) {
        MovieSort.SortData sortData;

        if (mouseEvent.getTarget() instanceof ListCell<?> listCell) {
            sortData = CastUtil.cast(listCell.getItem());
            if (sortData == null) {
                return;
            }
            loadMovieBySortData(sortData);
            putSortDataFilterListInMovieSortFilterPopOver(sortData);
        }
    }

    private void putSortDataFilterListInMovieSortFilterPopOver(MovieSort.SortData sortData) {
        boolean disableFlag = sortData.getFilters().isEmpty();

        movieSortFilterPopOver.putSortDataFilterList(sortData);
        if (classFilterButtonDisableProperty.get() != disableFlag) {
            classFilterButtonDisableProperty.set(disableFlag);
        }
    }

    private void setVideoGridShowSourceName(boolean flag) {
        VideoGridCellFactory factory = (VideoGridCellFactory) videosGridView.getCellFactory();

        factory.setShowSourceName(flag);
    }

    /**
     * 根据分类加载影片
     * @param sortData 分类数据
     */
    private void loadMovieBySortData(MovieSort.SortData sortData) {
        ObservableList<Movie.Video> items;
        MutablePair<Movie, List<Movie.Video>> movieAndVideosCached;

        resetMovieSearchService();
        movieLoadingProperty.set(true);
        items = videosGridView.getItems();
        if (!items.isEmpty()) {
            items.clear();
        }
        setVideoGridShowSourceName(false);
        movieAndVideosCached = MOVIE_CACHE.get(sortData.getId());
        if (movieAndVideosCached == null) {
            // 拉取影片数据
            template.getCategoryContent(
                    GetCategoryContentDTO.of(getSourceBean(), sortData, 1),
                    categoryContent -> {
                        Movie movie;
                        MutablePair<Movie, List<Movie.Video>> movieAndVideos;

                        if (categoryContent == null) {
                            movieLoadingProperty.set(false);

                            return;
                        }
                        movie = categoryContent.getMovie();
                        if (movie == null || CollectionUtil.isEmpty(movie.getVideoList())) {
                            movieLoadingProperty.set(false);

                            return;
                        }
                        movieAndVideos = MutablePair.of(movie, movie.getVideoList());
                        putVideosInView(movieAndVideos, true);
                        MOVIE_CACHE.put(sortData.getId(), movieAndVideos);
                        movieLoadingProperty.set(false);
                    }
            );
        } else {
            // 使用缓存中的影片数据
            putVideosInView(movieAndVideosCached, true);
            movieLoadingProperty.set(false);
        }
    }

    private void resetMovieSearchService() {
        movieSearchService.cancel();
        movieSearchService.reset();
        searchLoadingProperty.set(false);
    }

    /**
     * 将影片数据放入视图中
     * @param movieAndVideos 影片数据
     */
    private void putVideosInView(
            MutablePair<Movie, List<Movie.Video>> movieAndVideos,
            boolean showLoadMoreItem
    ) {
        ObservableList<Movie.Video> items = videosGridView.getItems();
        Movie movie = movieAndVideos.getLeft();
        List<Movie.Video> videos = movieAndVideos.getRight();

        if (videos.isEmpty()) {
            return;
        }
        Platform.runLater(() -> {
            items.addAll(videos);
            if (    showLoadMoreItem &&
                    !items.get(items.size() - 1).equals(fetchMoreItem) &&
                    movie.getPagecount() > movie.getPage() &&
                    movie.getRecordcount() > items.size()
            ) {
                // 添加“获取更多”项
                items.add(fetchMoreItem);
            }
        });
    }

    /**
     * 获取当前选中的源对象
     * @return 源对象
     */
    private SourceBean getSourceBean() {
        return sourceBeanComboBox.getSelectionModel().getSelectedItem();
    }

    /**
     * 根据源标识获取源对象
     * @param sourceKey 源ID
     * @return 源对象
     */
    private SourceBean getSourceBean(@Nullable String sourceKey) {
        if (sourceKey == null) {
            return getSourceBean();
        }

        return CollectionUtil.findFirst(
                sourceBeanComboBox.getItems(), sourceBean -> sourceBean.getKey().equals(sourceKey)
        ).orElseThrow(() -> new FBException("No source bean found for key: " + sourceKey));
    }

    /**
     * 销毁方法
     */
    @Override
    public void destroy() {
        ClientInfo clientInfo = clientManager.getCurrentClientImmediately();

        saveClientTVProperties();
        clearMovieData();
        if (videosGridView.getCellFactory() instanceof VideoGridCellFactory factory) {
            factory.destroy();
        }
        movieHistoryPopOver.destroy();
        movieCollectionPopOver.destroy();
        ImageHelper.clearCache();
        clientManager.clearCurrentClient();
        if (clientInfo != null) {
            log.info(
                    "[{}]'s tv controller destroy",
                    clientInfo.getName()
            );
        }
    }

    private void saveClientTVProperties() {
        SourceBean sourceBean = getSourceBean();

        if (sourceBean == null) {

            return;
        }
        clientTVProperties.setSourceKeyLastUsed(sourceBean.getKey());
        if (!clientTVPropertiesBackup.equals(clientTVProperties)) {
            StorageHelper.save(clientTVProperties);
        }
    }

    private void clearMovieData() {
        videosGridView.getItems().clear();
        MOVIE_CACHE.clear();
        resetMovieSearchService();
    }

    @FXML
    private void onSourceBeanBlockButtonAction() {
        sourceBeanBlockPopOver.show(sourceBeanBlockButton);
    }

    @FXML
    private void onHistoryBtnAction() {
        template.getPlayHistory(
                GetPlayHistoryDTO.of(100),
                playHistory -> Platform.runLater(() -> {
                    if (CollectionUtil.isNotEmpty(playHistory)) {
                        movieHistoryPopOver.setVodInfoList(playHistory);
                    }
                    movieHistoryPopOver.show(historyButton);
                })
        );
    }

    @FXML
    private void onCollectBtnAction() {
        template.getMovieCollection(vodCollects ->
            Platform.runLater(() -> {
                if (CollectionUtil.isNotEmpty(vodCollects)) {
                    movieCollectionPopOver.setVodInfoList(
                            vodCollects.stream().map(VodInfo::from).toList()
                    );
                }
                movieCollectionPopOver.show(collectButton);
            })
        );
    }

    @FXML
    private void onSearchTextFieldKeyPressed(KeyEvent evt) {
        if (evt.getCode() != KeyCode.ENTER) {
            return;
        }
        onSearchBtnAction();
    }

    @FXML
    private void onSearchBtnAction() {
        String searchKeyword = searchTextField.getText();
        Iterator<String> sourceBeanKeyIterator;

        if (StringUtils.isBlank(searchKeyword)) {
            return;
        }
        sourceBeanKeyIterator = sourceBeanComboBox.getItems()
                .stream()
                .filter(SourceBean::isSearchable)
                .map(SourceBean::getKey)
                .toList()
                .iterator();
        if (!sourceBeanKeyIterator.hasNext()) {
            return;
        }
        clearMovieData();
        setVideoGridShowSourceName(true);
        searchLoadingProperty.set(true);
        movieSearchService.setKeyword(searchKeyword);
        movieSearchService.setSourceKeyIterator(sourceBeanKeyIterator);
        movieSearchService.start();
    }

    @FXML
    private void onClassFilterBtnAction() {
        movieSortFilterPopOver.show(classFilterButton);
    }
}
