package io.knifer.freebox.controller;

import io.knifer.freebox.component.container.VLCPlayer;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.bo.VideoDetailsBO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;

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

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            VideoDetailsBO bo = getData();

            player = bo.getPlayer();
            player.play("E:\\下载\\齐泽克书单+课程删减视频\\齐泽克03（未删减版）.mp4");
            WindowHelper.getStage(root).setOnCloseRequest(evt -> {
                player.destroy();
                Context.INSTANCE.popAndShowLastStage();
            });
        });
    }
}
