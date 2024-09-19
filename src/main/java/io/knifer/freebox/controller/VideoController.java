package io.knifer.freebox.controller;

import com.google.gson.JsonElement;
import io.knifer.freebox.component.node.VLCPlayer;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.bo.VideoDetailsBO;
import io.knifer.freebox.model.common.Movie;
import io.knifer.freebox.model.common.SourceBean;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.s2c.GetPlayerContentDTO;
import io.knifer.freebox.net.websocket.template.KebSocketTemplate;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 影视详情控制器
 *
 * @author Knifer
 */
@Slf4j
public class VideoController extends BaseController {

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

    private Movie videoDetail;
    private SourceBean source;
    private VLCPlayer player;
    private KebSocketTemplate template;
    private ClientInfo clientInfo;

    private Button selectedEpBtn = null;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            VideoDetailsBO bo = getData();

            videoDetail = bo.getVideoDetail().getMovie();
            source = bo.getSource();
            player = bo.getPlayer();
            template = bo.getTemplate();
            clientInfo = bo.getClientInfo();
            if (videoDetail == null || videoDetail.getVideoList().isEmpty()) {
                ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_NO_DATA);
                return;
            }
            WindowHelper.getStage(root).setOnCloseRequest(evt -> close());
            videoDetailSplitPane.minHeightProperty().bind(root.heightProperty());
            putMovieDataInView();
            playFirstVideo();
        });
    }

    private void putMovieDataInView() {
        Movie.Video video = videoDetail.getVideoList().get(0);
        ObservableList<Node> detailsPropList = movieDetailsTextFlow.getChildren();
        int year = video.getYear();
        List<Movie.Video.UrlBean.UrlInfo> urlInfoList = video.getUrlBean().getInfoList();

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
        urlInfoList.forEach(urlInfo -> {
            String urlFlag = urlInfo.getFlag();
            Tab tab = new Tab(urlFlag);
            FlowPane flowPane = new FlowPane();
            ObservableList<Node> children = flowPane.getChildren();
            ScrollPane scrollPane = new ScrollPane(flowPane);

            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            flowPane.setHgap(10);
            flowPane.setVgap(10);
            flowPane.setAlignment(Pos.TOP_CENTER);
            flowPane.setPadding(new Insets(10, 0, 60, 0));
            urlInfo.getBeanList().forEach(bean -> {
                Button btn = new Button(bean.getName());

                children.add(btn);
                btn.setOnAction(evt -> {
                    if (selectedEpBtn != null) {
                        selectedEpBtn.getStyleClass().remove("video-details-ep-btn-selected");
                    }
                    selectedEpBtn = btn;
                    selectedEpBtn.getStyleClass().add("video-details-ep-btn-selected");
                    playVideo(video, urlFlag, bean.getUrl());
                });
            });
            tab.setContent(scrollPane);
            resourceTabPane.getTabs().add(tab);
        });
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

    private void playFirstVideo() {
        Movie.Video video = videoDetail.getVideoList().get(0);
        Movie.Video.UrlBean.UrlInfo urlInfo = video.getUrlBean().getInfoList().get(0);

        // 播放第一个视频
        playVideo(video, urlInfo.getFlag(), urlInfo.getBeanList().get(0).getUrl());
        // 设置第一个tab内的第一个按钮为选中状态
        selectedEpBtn = (
                (Button) ((FlowPane) ((ScrollPane) resourceTabPane.getTabs().get(0).getContent()).getContent())
                        .getChildren()
                        .get(0)
        );
        selectedEpBtn.getStyleClass().add("video-details-ep-btn-selected");
    }

    private void playVideo(Movie.Video video, String flag, String videoUrl) {
        template.getPlayerContent(
                clientInfo,
                GetPlayerContentDTO.of(video.getSourceKey(), StringUtils.EMPTY, flag, videoUrl),
                playerContentJson -> {
                    JsonElement nameValuePairs = playerContentJson.get("nameValuePairs");
                    JsonElement playUrl = nameValuePairs.getAsJsonObject().get("url");

                    if (playUrl == null) {
                        ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_NO_DATA);
                        return;
                    }
                    player.play(playUrl.getAsString());
                }
        );
    }

    private void close() {
        player.destroy();
        Context.INSTANCE.popAndShowLastStage();
    }
}
