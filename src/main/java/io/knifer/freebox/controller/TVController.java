package io.knifer.freebox.controller;

import io.knifer.freebox.component.converter.SourceBean2StringConverter;
import io.knifer.freebox.component.factory.ClassListCellFactory;
import io.knifer.freebox.component.factory.VideoGridCellFactory;
import io.knifer.freebox.component.factory.VodInfoGridCellFactory;
import io.knifer.freebox.component.node.MovieHistoryPopOver;
import io.knifer.freebox.component.node.VLCPlayer;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.Views;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.bo.VideoDetailsBO;
import io.knifer.freebox.model.bo.VideoPlayInfoBO;
import io.knifer.freebox.model.common.*;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.s2c.GetCategoryContentDTO;
import io.knifer.freebox.model.s2c.GetDetailContentDTO;
import io.knifer.freebox.model.s2c.GetPlayHistoryDTO;
import io.knifer.freebox.model.s2c.SavePlayHistoryDTO;
import io.knifer.freebox.net.websocket.core.KebSocketRunner;
import io.knifer.freebox.net.websocket.core.KebSocketTopicKeeper;
import io.knifer.freebox.net.websocket.template.KebSocketTemplate;
import io.knifer.freebox.net.websocket.template.impl.KebSocketTemplateImpl;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.FXMLUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.GridView;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 客户端电视
 *
 * @author Knifer
 */
@Slf4j
public class TVController extends BaseController {

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
    private Button historyButton;

    private final MovieHistoryPopOver movieHistoryPopOver = new MovieHistoryPopOver();

    private final BooleanProperty sortsLoadingProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty movieLoadingProperty = new SimpleBooleanProperty(false);
    private KebSocketTemplate template;
    private ClientInfo clientInfo;
    private Movie.Video fetchMoreItem;

    private final String HOME_SORT_DATA_ID = "ztx*1RcW6%Ep";

    private final Map<String, MutablePair<Movie, List<Movie.Video>>> MOVIE_CACHE = new HashMap<>();

    @FXML
    private void initialize() {
        template = new KebSocketTemplateImpl(new KebSocketRunner(KebSocketTopicKeeper.getInstance()));
        fetchMoreItem = new Movie.Video();
        fetchMoreItem.setId(BaseValues.LOAD_MORE_ITEM_ID);
        fetchMoreItem.setName(I18nHelper.get(I18nKeys.TV_LOAD_MORE));
        Platform.runLater(() -> {
            Stage stage = WindowHelper.getStage(root);

            clientInfo = getClientInfo();
            stage.setTitle(clientInfo.getConnection().getRemoteSocketAddress().getHostName());
            stage.setOnCloseRequest(evt -> {
               destroy();
               Context.INSTANCE.popAndShowLastStage();
            });

            // TODO converter写入FXML里
            sourceBeanComboBox.setConverter(new SourceBean2StringConverter());
            classesListView.setCellFactory(new ClassListCellFactory());
            videosGridView.setCellFactory(new VideoGridCellFactory());

            historyButton.disableProperty().bind(movieHistoryPopOver.showingProperty().or(movieLoadingProperty));
            movieHistoryPopOver.loadingPropertyProperty().bind(movieLoadingProperty);
            sortsLoadingProgressIndicator.visibleProperty().bind(sortsLoadingProperty);
            movieLoadingProgressIndicator.visibleProperty().bind(movieLoadingProperty);
            videosGridView.disableProperty().bind(movieLoadingProperty);
            classesListView.disableProperty().bind(sortsLoadingProperty);
            classesListView.disableProperty().bind(movieLoadingProperty);
            sourceBeanComboBox.disableProperty().bind(sortsLoadingProperty);

            movieHistoryPopOver.setOnVodInfoGridViewClicked(this::onVideosGridViewMouseClicked);

            template.getSourceBeanList(clientInfo, this::initSourceBeanData);
        });
    }

    /**
     * 影片视频列表项点击事件
     * @param evt 视频列表项点击事件
     */
    @FXML
    private void onVideosGridViewMouseClicked(MouseEvent evt) {
        EventTarget target = evt.getTarget();
        Movie.Video video;
        VodInfo vod;
        String videoId;

        if (target instanceof VideoGridCellFactory.VideoGridCell cell) {
            // TV界面影片
            video = cell.getItem();
            videoId = video.getId();
            if (videoId.equals(BaseValues.LOAD_MORE_ITEM_ID)) {
                loadMoreMovie(cell);
            } else if (evt.getClickCount() > 1) {
                openVideo(videoId, video.getName());
            }
        } else if (target instanceof VodInfoGridCellFactory.VodInfoGridCell cell && evt.getClickCount() > 1) {
            // 播放历史界面影片
            vod = cell.getItem();
            openVideo(vod.getId(), vod.getName(), VideoPlayInfoBO.of(vod));
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
                clientInfo,
                GetCategoryContentDTO.of(getCurrentSourceBean(), sortData, movieCached.getPage() + 1),
                categoryContent -> {
                    Movie movie = categoryContent.getMovie();
                    ObservableList<Movie.Video> items = videosGridView.getItems();
                    List<Movie.Video> videos = movie.getVideoList();
                    int loadMoreItemIdx;

                    if (videos.isEmpty()) {
                        return;
                    }
                    loadMoreItemIdx = items.size() - 1;
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
                }
        );
    }

    /**
     * 打开影片
     * @param videoId 影片ID
     * @param videoName 影片名称
     */
    private void openVideo(String videoId, String videoName) {
        openVideo(videoId, videoName, null);
    }


    /**
     * 打开影片
     * @param videoId 影片ID
     * @param videoName 影片名称
     * @param playInfo 播放信息
     */
    private void openVideo(String videoId, String videoName, @Nullable VideoPlayInfoBO playInfo) {
        boolean noPlayInfo = playInfo == null;
        String sourceKey;
        SourceBean sourceBean;

        if (noPlayInfo) {
            sourceBean = getCurrentSourceBean();
        } else {
            sourceKey = playInfo.getSourceKey();
            sourceBean = CollectionUtil.findFirst(sourceBeanComboBox.getItems(), s -> s.getKey().equals(sourceKey));
            if (sourceBean == null) {
                ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_MOVIE_DETAIL_FAILED);

                return;
            }
        }
        movieLoadingProperty.set(true);
        template.getDetailContent(
                clientInfo,
                GetDetailContentDTO.of(sourceBean.getKey(), videoId),
                detailContent -> {
                    Pair<Stage, VideoController> stageAndController;
                    Stage tvStage;
                    Stage videoStage;

                    if (detailContent == null) {
                        ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_MOVIE_DETAIL_FAILED);
                        movieLoadingProperty.set(false);

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
                            new VLCPlayer((HBox) videoStage.getScene().getRoot()),
                            template,
                            clientInfo,
                            newPlayInfo -> savePlayHistory(detailContent, newPlayInfo)
                    ));
                    Context.INSTANCE.pushStage(tvStage);
                    movieLoadingProperty.set(false);
                    tvStage.hide();
                    videoStage.show();
                }
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
        template.savePlayHistory(clientInfo, SavePlayHistoryDTO.of(vodInfo));
    }

    /**
     * 初始化源列表
     * @param sourceBeanList 源对象列表
     */
    private void initSourceBeanData(List<SourceBean> sourceBeanList) {
        List<SourceBean> items = sourceBeanComboBox.getItems();

        items.clear();
        if (sourceBeanList.isEmpty()) {
            sortsLoadingProperty.set(false);
            return;
        }
        items.addAll(sourceBeanList);
        sourceBeanComboBox.getSelectionModel().selectFirst();
    }

    /**
     * 源选择事件
     */
    @FXML
    private void onSourceBeanComboBoxAction() {
        ObservableList<MovieSort.SortData> items = classesListView.getItems();

        sortsLoadingProperty.set(true);
        items.clear();
        MOVIE_CACHE.clear();
        videosGridView.getItems().clear();
        template.getHomeContent(clientInfo, getCurrentSourceBean(), homeContent -> {
            Movie movie;
            List<Movie.Video> list;
            MovieSort classes;
            List<MovieSort.SortData> sortList;

            if (homeContent == null) {
                ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_SOURCE_FAILED);
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
                loadMovieBySortData(items.get(0));
            }
        });
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
        }
    }

    /**
     * 根据分类加载影片
     * @param sortData 分类数据
     */
    private void loadMovieBySortData(MovieSort.SortData sortData) {
        ObservableList<Movie.Video> items;
        MutablePair<Movie, List<Movie.Video>> movieAndVideosCached;

        movieLoadingProperty.set(true);
        items = videosGridView.getItems();
        if (!items.isEmpty()) {
            /*
             * 列表中原本有影片数据，在清空的同时，也要清空异步任务队列
             * 防止列表中旧有影片封面的异步加载任务占用异步线程
             */
            items.clear();
            AsyncUtil.cancelAllTask();
        }
        movieAndVideosCached = MOVIE_CACHE.get(sortData.getId());
        if (movieAndVideosCached == null) {
            // 拉取影片数据
            template.getCategoryContent(
                    clientInfo,
                    GetCategoryContentDTO.of(getCurrentSourceBean(), sortData, 1),
                    categoryContent -> {
                        Movie movie = categoryContent.getMovie();
                        MutablePair<Movie, List<Movie.Video>> movieAndVideos =
                                MutablePair.of(movie, movie.getVideoList());

                        putVideosInView(movieAndVideos);
                        MOVIE_CACHE.put(sortData.getId(), movieAndVideos);
                        movieLoadingProperty.set(false);
                    }
            );
        } else {
            // 使用缓存中的影片数据
            putVideosInView(movieAndVideosCached);
            movieLoadingProperty.set(false);
        }
    }

    /**
     * 将影片数据放入视图中
     * @param movieAndVideos 影片数据
     */
    private void putVideosInView(MutablePair<Movie, List<Movie.Video>> movieAndVideos) {
        ObservableList<Movie.Video> items = videosGridView.getItems();
        Movie movie = movieAndVideos.getLeft();
        List<Movie.Video> videos = movieAndVideos.getRight();

        if (videos.isEmpty()) {
            return;
        }
        items.addAll(videos);
        if (
                !items.get(items.size() - 1).equals(fetchMoreItem) &&
                movie.getPagecount() > movie.getPage() &&
                movie.getRecordcount() > items.size()
        ) {
            // 添加“获取更多”项
            items.add(fetchMoreItem);
        }
    }

    /**
     * 获取当前选中的源对象
     * @return 源对象
     */
    private SourceBean getCurrentSourceBean() {
        return sourceBeanComboBox.getSelectionModel().getSelectedItem();
    }

    /**
     * 销毁方法
     */
    private void destroy() {
        log.info(
                "[{}]'s tv controller destroy",
                clientInfo.getConnection().getRemoteSocketAddress().getHostName()
        );
        if (videosGridView.getCellFactory() instanceof VideoGridCellFactory factory) {
            factory.destroy();
        }
        MOVIE_CACHE.clear();
    }

    private ClientInfo getClientInfo() {
        return getData();
    }

    @FXML
    private void onHistoryBtnAction() {
        template.getPlayHistory(clientInfo, GetPlayHistoryDTO.of(100), playHistory -> {
            if (CollectionUtil.isNotEmpty(playHistory)) {
                movieHistoryPopOver.setVodInfoList(playHistory);
            }
            movieHistoryPopOver.show(historyButton);
        });
    }
}
