package io.knifer.freebox.controller;

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

    private VideoDetailsBO bo;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            bo = getData();
            root.setOnMouseClicked(evt -> {
                // TODO play
                bo.getPlayer().play("");
            });
            WindowHelper.getStage(root).setOnCloseRequest(evt -> Context.INSTANCE.popAndShowLastStage());
        });
    }
}
