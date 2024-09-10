package io.knifer.freebox.controller;

import com.google.gson.JsonElement;
import io.knifer.freebox.component.node.VLCPlayer;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.bo.VideoDetailsBO;
import io.knifer.freebox.model.common.Movie;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.s2c.GetPlayerContentDTO;
import io.knifer.freebox.net.websocket.template.KebSocketTemplate;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 影视详情控制器
 *
 * @author Knifer
 */
@Slf4j
public class VideoController extends BaseController {

    @FXML
    private BorderPane root;

    private VLCPlayer player;

    private Movie videoDetail;

    private KebSocketTemplate template;

    private ClientInfo clientInfo;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            VideoDetailsBO bo = getData();

            videoDetail = bo.getVideoDetail().getMovie();
            player = bo.getPlayer();
            template = bo.getTemplate();
            clientInfo = bo.getClientInfo();
            if (videoDetail == null || videoDetail.getVideoList().isEmpty()) {
                throw new FBException(I18nHelper.get(I18nKeys.VIDEO_ERROR_NO_DATA));
            }
            WindowHelper.getStage(root).setOnCloseRequest(evt -> close());
            playFirstVideo();
        });
    }

    private void playFirstVideo() {
        Movie.Video video = videoDetail.getVideoList().get(0);
        Movie.Video.UrlBean.UrlInfo urlInfo = video.getUrlBean().getInfoList().get(0);

        template.getPlayerContent(
                clientInfo,
                GetPlayerContentDTO.of(
                        video.getSourceKey(),
                        StringUtils.EMPTY,
                        urlInfo.getFlag(),
                        urlInfo.getBeanList().get(0).getUrl()
                ),
                playerContentJson -> {
                    JsonElement nameValuePairs = playerContentJson.get("nameValuePairs");
                    JsonElement url = nameValuePairs.getAsJsonObject().get("url");

                    if (url == null) {
                        throw new FBException(I18nHelper.get(I18nKeys.VIDEO_ERROR_NO_DATA));
                    }
                    player.play(url.getAsString());
                }
        );
    }

    private void close() {
        player.destroy();
        Context.INSTANCE.popAndShowLastStage();
    }
}
