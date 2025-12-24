package io.knifer.freebox.controller;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.knifer.freebox.component.node.player.BasePlayer;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.CacheKeys;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.handler.M3u8AdFilterHandler;
import io.knifer.freebox.handler.M3u8TsProxyHandler;
import io.knifer.freebox.handler.impl.BadM3u8TsProxyHandler;
import io.knifer.freebox.handler.impl.SmartM3u8AdFilterHandler;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.bo.TVPlayBO;
import io.knifer.freebox.model.bo.VideoDetailsBO;
import io.knifer.freebox.model.bo.VideoPlayInfoBO;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.model.common.tvbox.VodInfo;
import io.knifer.freebox.model.domain.M3u8AdFilterResult;
import io.knifer.freebox.model.s2c.DeleteMovieCollectionDTO;
import io.knifer.freebox.model.s2c.GetMovieCollectedStatusDTO;
import io.knifer.freebox.model.s2c.GetPlayerContentDTO;
import io.knifer.freebox.model.s2c.SaveMovieCollectionDTO;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.annotation.Nullable;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 影视详情控制器
 *
 * @author Knifer
 */
@Slf4j
public class VideoController extends BaseController implements Destroyable {

    @FXML
    private HBox root;
    @FXML
    private SplitPane videoDetailSplitPane;
    @FXML
    private Label movieTitleLabel;
    @FXML
    private TextFlow movieDetailsTextFlow;
    @FXML
    private TabPane resourceTabPane;
    @FXML
    private Button collectBtn;

    private final FontIcon COLLECT_FONT_ICON = FontIcon.of(FontAwesome.STAR_O, 16, Color.ORANGE);
    private final FontIcon COLLECTED_FONT_ICON = FontIcon.of(FontAwesome.STAR, 16, Color.ORANGE);

    private Movie videoDetail;
    private VideoPlayInfoBO playInfo;
    private SourceBean source;
    private BasePlayer<?> player;
    private SpiderTemplate template;
    private Consumer<VideoPlayInfoBO> onClose;

    private M3u8AdFilterHandler m3u8AdFilterHandler;
    private M3u8TsProxyHandler m3u8TsProxyHandler;

    private Button selectedEpBtn = null;
    private Movie.Video playingVideo;
    private Movie.Video.UrlBean.UrlInfo playingUrlInfo;
    private Movie.Video.UrlBean.UrlInfo.InfoBean playingInfoBean;
    public final BooleanProperty operationLoading = new SimpleBooleanProperty(true);

    private static final Set<String> HTTP_HEADERS_PROXY_EXCLUDE = Set.of(
            "content-length",
            "Content-Length",
            "transfer-encoding",
            "Transfer-Encoding"
    );

    @FXML
    private void initialize() {
        m3u8AdFilterHandler = new SmartM3u8AdFilterHandler();
        m3u8TsProxyHandler = new BadM3u8TsProxyHandler();
        Platform.runLater(() -> {
            VideoDetailsBO bo = getData();

            if (bo == null) {
                ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_NO_DATA);

                return;
            }
            videoDetail = bo.getVideoDetail().getMovie();
            playInfo = bo.getPlayInfo();
            source = bo.getSource();
            player = bo.getPlayer();
            template = bo.getTemplate();
            onClose = bo.getOnClose();
            if (videoDetail == null || videoDetail.getVideoList().isEmpty()) {
                ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_NO_DATA);
                return;
            }
            // 收藏按钮
            collectBtn.disableProperty().bind(operationLoading);
            template.getMovieCollectedStatus(
                    GetMovieCollectedStatusDTO.of(source.getKey(), videoDetail.getVideoList().get(0).getId()),
                    this::setCollectBtnByCollectedStatus
            );
            // 绑定播放下一集事件
            player.setOnStepForward(this::onPlayerStepForward);
            WindowHelper.getStage(root).setOnCloseRequest(evt -> destroy());
            videoDetailSplitPane.minHeightProperty().bind(root.heightProperty());
            putMovieDataInView();
            startPlayVideo();
        });
    }

    /**
     * 播放下一集
     */
    private void onPlayerStepForward() {
        Iterator<Movie.Video.UrlBean.UrlInfo.InfoBean> beanIter;
        Movie.Video.UrlBean.UrlInfo.InfoBean bean;
        ObservableList<Node> epBtnList;
        Iterator<Node> epBtnIter;
        Button epBtn;

        if (playingVideo == null) {
            return;
        }
        beanIter = playingUrlInfo.getBeanList().iterator();
        while (beanIter.hasNext()) {
            bean = beanIter.next();
            if (bean.getUrl().equals(playingInfoBean.getUrl())) {
                if (beanIter.hasNext()) {
                    // 准备播放下一集，先更新“被选中的当前集按钮”样式
                    epBtnList = ((FlowPane) (
                            (ScrollPane) resourceTabPane.getSelectionModel().getSelectedItem().getContent()
                    ).getContent()).getChildren();
                    epBtnIter = epBtnList.iterator();
                    while (epBtnIter.hasNext()) {
                        epBtn = (Button) epBtnIter.next();
                        if (epBtn == selectedEpBtn) {
                            updateSelectedEpBtn(((Button) epBtnIter.next()));
                            break;
                        }
                    }
                    // 播放下一集，同时更新播放信息
                    playVideo(playingVideo, playingUrlInfo, beanIter.next(), null);
                } else {
                    Platform.runLater(() ->
                            player.showToast(I18nHelper.get(I18nKeys.VIDEO_INFO_NO_MORE_EP))
                    );
                }
                break;
            }
        }
    }

    private void putMovieDataInView() {
        Movie.Video video = videoDetail.getVideoList().get(0);
        ObservableList<Node> detailsPropList = movieDetailsTextFlow.getChildren();
        int year = video.getYear();
        List<Movie.Video.UrlBean.UrlInfo> urlInfoList = video.getUrlBean().getInfoList();
        boolean hasPlayInfo = playInfo != null;
        String playFlag;
        List<Tab> tabs;

        // 影片信息
        movieTitleLabel.setText(video.getName());
        addMovieDetailsIfExists(detailsPropList, I18nKeys.VIDEO_MOVIE_DETAILS_SOURCE, source.getName());
        if (year != 0) {
            addMovieDetailsIfExists(detailsPropList, I18nKeys.VIDEO_MOVIE_DETAILS_YEAR, String.valueOf(year));
        }
        addMovieDetailsIfExists(detailsPropList, I18nKeys.VIDEO_MOVIE_DETAILS_AREA, video.getArea());
        addMovieDetailsIfExists(detailsPropList, I18nKeys.VIDEO_MOVIE_DETAILS_TYPE, video.getType());
        addMovieDetailsIfExists(detailsPropList, I18nKeys.VIDEO_MOVIE_DETAILS_DIRECTORS, video.getDirector());
        addMovieDetailsIfExists(detailsPropList, I18nKeys.VIDEO_MOVIE_DETAILS_ACTORS, video.getActor());
        addMovieDetailsIfExists(detailsPropList, I18nKeys.VIDEO_MOVIE_DETAILS_LINK, video.getId());
        addMovieDetailsIfExists(detailsPropList, I18nKeys.VIDEO_MOVIE_DETAILS_INTRO, video.getDes());
        // 选集信息
        if (urlInfoList.isEmpty()) {
            ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_NO_RESOURCE);
            return;
        }
        tabs = resourceTabPane.getTabs();
        if (hasPlayInfo) {
            setPlayIndexIfNeeded(playInfo, videoDetail);
            playFlag = ObjectUtils.defaultIfNull(playInfo.getPlayFlag(), StringUtils.EMPTY);
        } else {
            playFlag = StringUtils.EMPTY;
        }
        urlInfoList.forEach(urlInfo -> {
            String urlFlag = urlInfo.getFlag();
            List<Movie.Video.UrlBean.UrlInfo.InfoBean> beanList = urlInfo.getBeanList();
            Tab tab = new Tab(urlFlag);
            FlowPane flowPane = new FlowPane();
            ObservableList<Node> children = flowPane.getChildren();
            ScrollPane scrollPane = new ScrollPane(flowPane);
            CheckMenuItem reverseMenuItem;

            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            flowPane.setHgap(10);
            flowPane.setVgap(10);
            flowPane.setAlignment(Pos.TOP_CENTER);
            flowPane.setPadding(new Insets(10, 0, 60, 0));
            if (hasPlayInfo && urlFlag.equals(playInfo.getPlayFlag()) && playInfo.isReverseSort()) {
                Collections.reverse(beanList);
            }
            beanList.forEach(bean -> {
                Button btn = new Button(bean.getName());

                children.add(btn);
                btn.setOnAction(evt -> {
                    if (btn == selectedEpBtn) {
                        return;
                    }
                    // 选集按钮被点击，更新样式，并播放对应选集集视频
                    updateSelectedEpBtn(btn);
                    playVideo(video, urlInfo, bean, null);
                });
            });
            tab.setContent(scrollPane);
            // 给选集标签页绑定右键菜单
            reverseMenuItem = new CheckMenuItem(I18nHelper.get(I18nKeys.VIDEO_REVERSE));
            reverseMenuItem.setOnAction(evt -> {
                /* 倒序操作 */
                // 数据列表倒叙
                Collections.reverse(beanList);
                // 按钮列表倒序
                FXCollections.reverse(children);
            });
            tab.setContextMenu(new ContextMenu(reverseMenuItem));
            if (hasPlayInfo && playFlag.equals(urlFlag)) {
                // 存在历史记录，且历史记录的选集标签页与当前播放的选集标签页相同，因此要赋予当前选集标签页历史记录中的相关属性
                reverseMenuItem.setSelected(playInfo.isReverseSort());
            }
            // 添加标签页
            tabs.add(tab);
        });
    }

    private void updateSelectedEpBtn(Button newSelectedEpBtn) {
        if (selectedEpBtn != null) {
            selectedEpBtn.getStyleClass().remove("video-details-ep-btn-selected");
        }
        selectedEpBtn = newSelectedEpBtn;
        selectedEpBtn.getStyleClass().add("video-details-ep-btn-selected");
    }

    private void addMovieDetailsIfExists(
            ObservableList<Node> children,
            String propNameI18nKey,
            String propValue
    ) {
        Text propValueText;
        Text propNameText;
        Tooltip tooltip;

        if (StringUtils.isBlank(propValue)) {
            return;
        }
        propValue = StringUtils.trim(propValue);
        if (!children.isEmpty()) {
            children.add(new Text("\n"));
        }
        propNameText = new Text(I18nHelper.get(propNameI18nKey));
        propNameText.getStyleClass().add("video-details-prop-name");
        if (propValue.length() > 50) {
            propValueText = new Text(propValue.substring(0, 30) + ".....");
            tooltip = new Tooltip(propValue);
            tooltip.setPrefWidth(250);
            tooltip.setWrapText(true);
            Tooltip.install(propValueText, tooltip);
        } else {
            propValueText = new Text(propValue);
        }
        children.add(propNameText);
        children.add(propValueText);
        movieDetailsTextFlow.setMinHeight(
                movieDetailsTextFlow.getHeight() + propValueText.getFont().getSize()
        );
    }

    /**
     * 如有必要，设置要恢复播放的集数索引
     * 在与FongMi TV配对时，由于其提供的历史记录中没有playIndex，因此需要通过playNote来匹配
     * @param playInfo 播放信息
     * @param movie 影视数据
     */
    private void setPlayIndexIfNeeded(VideoPlayInfoBO playInfo, Movie movie) {
        int playIndex = playInfo.getPlayIndex();
        List<Movie.Video> videos;
        Movie.Video.UrlBean urlBean;
        List<Movie.Video.UrlBean.UrlInfo> infoList;
        int infoListSize;
        Movie.Video.UrlBean.UrlInfo urlInfo;
        String playFlag;
        final String finalPlayFlag;
        String playNote;

        if (playIndex > 0) {

            return;
        }
        videos = movie.getVideoList();
        urlBean = videos.get(0).getUrlBean();
        infoList = urlBean.getInfoList();
        infoListSize = CollUtil.size(infoList);
        if (infoListSize == 0) {

            return;
        }
        playFlag = playInfo.getPlayFlag();
        if (playFlag == null || infoListSize == 1) {
            playFlag = infoList.get(0).getFlag();
        }
        playNote = playInfo.getPlayNote();
        if (playNote == null) {

            return;
        }
        finalPlayFlag = playFlag;
        urlInfo = CollectionUtil.findFirst(infoList, info -> finalPlayFlag.equals(info.getFlag()))
                .orElse(null);
        if (urlInfo == null) {

            return;
        }
        playIndex = CollUtil.indexOf(urlInfo.getBeanList(), urlB -> playNote.equals(urlB.getName()));
        if (playIndex != -1) {
            playInfo.setPlayIndex(playIndex);
        }
    }

    private void startPlayVideo() {
        Movie.Video video = videoDetail.getVideoList().get(0);
        Movie.Video.UrlBean.UrlInfo urlInfo;
        List<Movie.Video.UrlBean.UrlInfo.InfoBean> beanList;
        Movie.Video.UrlBean.UrlInfo.InfoBean infoBean;
        String playFlag;
        int playIndex;
        int finalPlayIndex;
        Long progress = null;

        if (playInfo == null) {
            // 没有附带播放信息，直接播放第一个视频
            urlInfo = video.getUrlBean().getInfoList().get(0);
            infoBean = urlInfo.getBeanList().get(0);
            // 设置第一个tab内的第一个按钮为选中状态
            selectedEpBtn = (
                    (Button) ((FlowPane) ((ScrollPane) resourceTabPane.getTabs().get(0).getContent()).getContent())
                            .getChildren()
                            .get(0)
            );
        } else {
            playFlag = ObjectUtils.defaultIfNull(playInfo.getPlayFlag(), StringUtils.EMPTY);
            urlInfo = CollectionUtil.findFirst(
                    video.getUrlBean().getInfoList(), info -> playFlag.equals(info.getFlag())
            ).orElseGet(() -> video.getUrlBean().getInfoList().get(0));
            playIndex = playInfo.getPlayIndex();
            beanList = urlInfo.getBeanList();
            if (playIndex < 0 || beanList.size() - 1 < playIndex) {
                playIndex = 0;
            }
            infoBean = beanList.get(playIndex);
            finalPlayIndex = playIndex;
            // 设置指定选集按钮为选中状态
            CollectionUtil.findFirst(resourceTabPane.getTabs(), t -> t.getText().equals(playFlag))
                    .ifPresentOrElse(tab -> selectedEpBtn = (
                            (Button) ((FlowPane) ((ScrollPane) tab.getContent()).getContent())
                                    .getChildren()
                                    .get(finalPlayIndex)
                    ), () -> selectedEpBtn = (
                            (Button) ((FlowPane) ((ScrollPane) resourceTabPane.getTabs().get(0).getContent())
                                    .getContent())
                                    .getChildren()
                                    .get(0)
                    ));
            progress = playInfo.getProgress();
        }
        selectedEpBtn.getStyleClass().add("video-details-ep-btn-selected");
        playVideo(video, urlInfo, infoBean, progress);
    }

    /**
     * 播放视频
     * @param video 影视信息
     * @param urlInfo 播放源信息
     * @param urlInfoBean 播放集数
     * @param progress 播放进度（为空则从0播放）
     */
    private void playVideo(
            Movie.Video video,
            Movie.Video.UrlBean.UrlInfo urlInfo,
            Movie.Video.UrlBean.UrlInfo.InfoBean urlInfoBean,
            @Nullable Long progress
    ) {
        String flag = urlInfo.getFlag();

        Platform.runLater(() -> player.stop());
        template.getPlayerContent(
                GetPlayerContentDTO.of(video.getSourceKey(), flag, urlInfoBean.getUrl()),
                playerContentJson ->
                    Platform.runLater(() -> {
                        JsonElement propsElm;
                        JsonObject propsObj;
                        JsonElement elm;
                        String playUrl;
                        int parse;
                        int jx;
                        Map<String, String> headers;
                        String videoTitle;

                        if (playerContentJson == null) {
                            ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_NO_DATA);
                            return;
                        }
                        propsElm = playerContentJson.get("nameValuePairs");
                        if (propsElm == null) {
                            ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_NO_DATA);
                            return;
                        }
                        propsObj = propsElm.getAsJsonObject();
                        elm = propsObj.get("url");
                        if (elm == null) {
                            ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_NO_DATA);
                            return;
                        }
                        playUrl = elm.getAsString();
                        if (StringUtils.isBlank(playUrl)) {
                            ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_NO_DATA);
                            return;
                        }
                        playUrl = URLDecoder.decode(playUrl, Charsets.UTF_8);
                        elm = propsObj.get("parse");
                        parse = elm == null ? 0 : elm.getAsInt();
                        elm = propsObj.get("jx");
                        jx = elm == null ? 0 : elm.getAsInt();
                        elm = propsObj.get("header");
                        if (elm == null) {
                            headers = Map.of();
                        } else {
                            headers = Maps.transformValues(
                                    GsonUtil.fromJson(elm.getAsString(), JsonObject.class).asMap(),
                                    JsonElement::getAsString
                            );
                        }
                        videoTitle = "《" + video.getName() + "》" + flag + " - " + urlInfoBean.getName();
                        if (parse == 0) {
                            if (ConfigHelper.getAdFilter() && playUrl.contains(".m3u8")) {
                                // 处理m3u8广告过滤、ts代理
                                filterAdAndProxy(playUrl, headers, isAdFilteredAndProxyUrl -> {
                                    Boolean isAdFiltered = isAdFilteredAndProxyUrl.getLeft();
                                    String proxyUrl = isAdFilteredAndProxyUrl.getRight();
                                    TVPlayBO tvPlayBO = TVPlayBO.of(
                                            proxyUrl,
                                            headers,
                                            videoTitle,
                                            progress,
                                            isAdFiltered
                                    );

                                    Platform.runLater(() -> player.play(tvPlayBO));
                                });
                            } else {
                                player.play(TVPlayBO.of(
                                        playUrl, headers, videoTitle, progress, false
                                ));
                            }
                        } else {
                            if (jx != 0) {
                                ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_SOURCE_NOT_SUPPORTED);
                                return;
                            }
                            player.setVideoTitle(videoTitle + " （此为解析源，请在弹出的浏览器程序中观看）");
                            HostServiceHelper.showDocument(playUrl);
                        }
                        playingVideo = video;
                        playingUrlInfo = urlInfo;
                        playingInfoBean = urlInfoBean;
                    })
        );
    }

    /**
     * 过滤广告并创建本地代理链接
     * @param playUrl 播放链接
     * @param headers 请求源m3u8需要携带的请求头
     * @param callback 回调。包含过滤广告成功标志和代理链接
     */
    private void filterAdAndProxy(
            String playUrl, Map<String, String> headers, Consumer<Pair<Boolean, String>> callback
    ) {
        AsyncUtil.execute(() -> {
            HttpRequest.Builder requestBuilder;
            M3u8AdFilterResult result;
            String content;
            HttpResponse<String> resp;
            Map<String, List<String>> proxyHeaders = null;
            boolean isAdFiltered = false;
            String playUrlForTsProxy;
            String proxyUrlPrefix;
            Pair<Boolean, String> proxyTsFlagAndProxiedM3u8Content;
            String resultPlayUrl;

            requestBuilder = HttpRequest.newBuilder()
                    .GET()
                    .headers(HttpHeaders.USER_AGENT, BaseValues.USER_AGENT)
                    .uri(HttpUtil.parseUrl(playUrl));
            if (!headers.isEmpty()) {
                headers.forEach(requestBuilder::header);
            }
            try {
                resp = HttpUtil.getClient()
                        .sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                        .get(6, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 下载m3u8失败，直接返回原地址
                callback.accept(Pair.of(false, playUrl));

                return;
            }
            proxyUrlPrefix = createProxyUrlPrefix();
            // 处理m3u8广告过滤
            try {
                content = resp.body();
                result = m3u8AdFilterHandler.handle(
                        playUrl,
                        content,
                        Map.of(SmartM3u8AdFilterHandler.EXTRA_KEY_DTF, ConfigHelper.getAdFilterDynamicThresholdFactor())
                );
                isAdFiltered = result.getAdLineCount() > 0;
                if (isAdFiltered) {
                    content = result.getContent();
                    proxyHeaders = Maps.filterKeys(
                            resp.headers().map(), key -> !HTTP_HEADERS_PROXY_EXCLUDE.contains(key)
                    );
                }
                playUrlForTsProxy = isAdFiltered ? proxyM3u8(content, proxyUrlPrefix, proxyHeaders) : playUrl;
            } catch (Exception e) {
                log.warn("filter ad exception", e);
                content = resp.body();
                playUrlForTsProxy = playUrl;
            }
            // 处理损坏文件头的ts代理
            try {
                proxyTsFlagAndProxiedM3u8Content =
                        m3u8TsProxyHandler.handle(
                                playUrlForTsProxy, content, proxyUrlPrefix + "/proxy/ts/"
                        );
                if (proxyTsFlagAndProxiedM3u8Content.getLeft()) {
                    content = proxyTsFlagAndProxiedM3u8Content.getRight();
                    if (proxyHeaders == null) {
                        proxyHeaders = Maps.filterKeys(
                                resp.headers().map(), key -> !HTTP_HEADERS_PROXY_EXCLUDE.contains(key)
                        );
                    }
                    resultPlayUrl = proxyM3u8(content, proxyUrlPrefix, proxyHeaders);
                } else {
                    resultPlayUrl = playUrlForTsProxy;
                }
            } catch (Exception e) {
                log.warn("proxy ts exception", e);
                resultPlayUrl = playUrlForTsProxy;
            }
            callback.accept(Pair.of(isAdFiltered, resultPlayUrl));
        });
    }

    private String createProxyUrlPrefix() {
        return "http://127.0.0.1:" + ConfigHelper.getHttpPort();
    }

    /**
     * 代理m3u8内容
     * @param m3u8Content m3u8内容
     * @param proxyUrlPrefix 代理前缀
     * @param proxyHeaders 代理请求头
     * @return 代理链接
     */
    private String proxyM3u8(String m3u8Content, String proxyUrlPrefix, Map<String, List<String>> proxyHeaders) {
        String proxyUrl = proxyUrlPrefix + "/proxy-cache/" + CacheKeys.AD_FILTERED_M3U8;

        CacheHelper.put(CacheKeys.AD_FILTERED_M3U8, m3u8Content);
        CacheHelper.put(
                CacheKeys.PROXY_CACHE_HTTP_HEADERS + CacheKeys.AD_FILTERED_M3U8,
                proxyHeaders
        );

        return proxyUrl;
    }

    @Override
    public void destroy() {
        AsyncUtil.execute(() -> {
            updatePlayInfo();
            onClose.accept(playInfo);
            player.destroy();
        });
        Context.INSTANCE.popAndShowLastStage();
    }

    private void updatePlayInfo() {
        String playFlag;

        if (playingVideo == null || playingUrlInfo == null || playingInfoBean == null) {
            return;
        }
        playFlag = playingUrlInfo.getFlag();
        if (playInfo == null) {
            playInfo = new VideoPlayInfoBO();
        }
        playInfo.setPlayFlag(playFlag);
        playInfo.setPlayIndex(playingUrlInfo.getBeanList().indexOf(playingInfoBean));
        playInfo.setProgress(player.getCurrentProgress());
        playInfo.setDuration(player.getCurrentDuration());
        playInfo.setPlayNote(selectedEpBtn.getText());
        CollectionUtil.findFirst(resourceTabPane.getTabs(), tab -> tab.getText().equals(playFlag))
                .ifPresent(tab -> {
                    ObservableList<MenuItem> menus = tab.getContextMenu().getItems();
                    CheckMenuItem reverseMenuItem = (CheckMenuItem) menus.get(0);

                    playInfo.setReverseSort(reverseMenuItem.isSelected());
                });
    }

    @FXML
    private void onCollectBtnAction() {
        VodInfo vodInfo;

        if (playingVideo == null) {

            return;
        }
        operationLoading.set(true);
        vodInfo = VodInfo.from(playingVideo);
        if (collectBtn.getGraphic() == COLLECTED_FONT_ICON) {
            template.deleteMovieCollection(
                    DeleteMovieCollectionDTO.of(vodInfo),
                    () -> {
                        setCollectBtnByCollectedStatus(false);
                        Platform.runLater(() -> ToastHelper.showSuccessI18n(I18nKeys.COMMON_MESSAGE_SUCCESS));
                    }
            );
        } else {
            template.saveMovieCollection(
                    SaveMovieCollectionDTO.of(vodInfo),
                    () -> {
                        setCollectBtnByCollectedStatus(true);
                        Platform.runLater(() -> ToastHelper.showSuccessI18n(I18nKeys.COMMON_MESSAGE_SUCCESS));
                    }
            );
        }
    }

    private void setCollectBtnByCollectedStatus(@Nullable Boolean collectedStatus) {
        Platform.runLater(() -> {
            if (BooleanUtils.toBoolean(collectedStatus)) {
                collectBtn.setGraphic(COLLECTED_FONT_ICON);
                collectBtn.setText(I18nHelper.get(I18nKeys.VIDEO_UN_COLLECT));
            } else {
                collectBtn.setGraphic(COLLECT_FONT_ICON);
                collectBtn.setText(I18nHelper.get(I18nKeys.VIDEO_COLLECT));
            }
            operationLoading.set(false);
        });
    }
}
