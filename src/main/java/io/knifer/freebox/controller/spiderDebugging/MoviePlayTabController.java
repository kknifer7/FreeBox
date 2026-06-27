package io.knifer.freebox.controller.spiderDebugging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.knifer.freebox.component.node.player.BasePlayer;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.bo.TVPlayBO;
import io.knifer.freebox.spider.js.JSSpider;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * 爬虫调试 - 影视播放
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public class MoviePlayTabController extends SpiderDebuggingTabController{

    @FXML
    private VBox root;
    @FXML
    private CheckBox moviePlayTabAutoRefreshCheckBox;
    @FXML
    @Getter
    private TextField vodIdTextField;
    @FXML
    private Button loadPlayerContentButton;
    @FXML
    private ProgressIndicator moviePlayTabLoadingProgressIndicator;
    @FXML
    private TextField playUrlTextField;
    @FXML
    private Button playButton;
    @FXML
    private HBox playerPane;

    private BooleanProperty moviePlayLoadingProperty;
    private int moviePlayReloadSerial;

    private BasePlayer<?> player;

    private JsonObject playerContentJson;

    private final Context context;

    @FXML
    private void initialize() {
        moviePlayLoadingProperty = new SimpleBooleanProperty(false);
        Platform.runLater(() -> {
            Stage stage = WindowHelper.getStage(root);
            ReadOnlyDoubleProperty stageWidthProperty = stage.widthProperty();
            ReadOnlyDoubleProperty stageHeightProperty = stage.heightProperty();
            DoubleBinding playerPaneHeightProperty = stageHeightProperty.divide(2);
            Insets rootPadding = new Insets(10);

            stage.addEventFilter(
                    WindowEvent.WINDOW_HIDDEN,
                    ignored -> {
                        if (player != null) {
                            AsyncUtil.execute(() -> player.destroy());
                        }
                    }
            );
            root.setPadding(rootPadding);
            bindPlayerSize(stageWidthProperty, playerPaneHeightProperty);
            playButton.disableProperty().bind(playUrlTextField.textProperty().isEmpty());
            player = BasePlayer.createPlayer(
                    playerPane,
                    BasePlayer.Config.builder().liveMode(false).allowFullScreen(false).build()
            );
        });
        context.registerEventListener(AppEvents.SpiderDebuggingViewInitialized.class, evt -> {
            applyParentController(evt.controller());
            moviePlayTabLoadingProgressIndicator.visibleProperty()
                    .bind(spiderLoadingProperty.or(moviePlayLoadingProperty));
            loadPlayerContentButton.disableProperty().bind(
                    spiderLoadingProperty.or(moviePlayLoadingProperty.or(vodIdTextField.textProperty().isEmpty()))
            );
            parentController.setMoviePlayTabController(this);
        });
    }

    private void bindPlayerSize(DoubleExpression widthProp, DoubleExpression heightProp) {
        DoubleProperty minWidthProp = playerPane.minWidthProperty();
        DoubleProperty maxWidthProp = playerPane.maxWidthProperty();
        DoubleProperty minHeightProp = playerPane.minHeightProperty();
        DoubleProperty maxHeightProp = playerPane.maxHeightProperty();

        minWidthProp.bind(widthProp);
        maxWidthProp.bind(widthProp);
        minHeightProp.bind(heightProp);
        maxHeightProp.bind(heightProp);
    }

    @FXML
    private void onPlayButtonAction() {
        JsonElement elm;
        String playUrl;
        int parse;
        int jx;
        Map<String, String> headers;
        String videoTitle = I18nHelper.get(I18nKeys.SPIDER_DEBUGGING_MOVIE_PLAY_VIDEO_TITLE);

        if (playerContentJson == null) {
            ToastHelper.showErrorI18n(I18nKeys.SPIDER_DEBUGGING_MOVIE_PLAY_NO_VIDEO_DATA);
            return;
        }
        elm = playerContentJson.get("url");
        if (elm == null) {
            ToastHelper.showErrorI18n(I18nKeys.SPIDER_DEBUGGING_MOVIE_PLAY_NO_VIDEO_DATA);
            return;
        }
        playUrl = elm.getAsString();
        if (StringUtils.isBlank(playUrl)) {
            ToastHelper.showErrorI18n(I18nKeys.SPIDER_DEBUGGING_MOVIE_PLAY_NO_VIDEO_DATA);
            return;
        }
        elm = playerContentJson.get("parse");
        parse = elm == null ? 0 : elm.getAsInt();
        elm = playerContentJson.get("jx");
        jx = elm == null ? 0 : elm.getAsInt();
        elm = playerContentJson.get("header");
        headers = elm == null ? Map.of() : GsonUtil.toStringMap(elm);
        if (parse == 0) {
            player.play(TVPlayBO.of(playUrl, headers, videoTitle, null, false));
        } else {
            if (jx != 0) {
                ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_SOURCE_NOT_SUPPORTED);
                return;
            }
            player.setVideoTitle(videoTitle + " （此为解析源，请在弹出的浏览器程序中观看）");
            HostServiceHelper.showDocument(playUrl);
        }
    }

    @FXML
    private void onLoaderPlayerContentButtonAction() {
        reload();
    }


    @Override
    public void reload() {
        String vodId = vodIdTextField.getText();
        int currentSerial;

        moviePlayLoadingProperty.set(true);
        playerContentJson = null;
        if (player != null) {
            player.stop();
        }
        if (StringUtils.isEmpty(vodId)) {
            completeReloading();

            return;
        }
        currentSerial = ++moviePlayReloadSerial;
        spiderPreviewTaskMap.compute(SourceAuditType.MOVIE_PLAY, (k, spiderPreviewTask) -> {
            if (spiderPreviewTask != null) {
                parentController.cancelSpiderTaskIfNeeded(spiderPreviewTask);
            }

            return spiderPreviewExecutor.submit(() -> {
                JSSpider localSpider;
                String resultJson;
                JsonObject result;
                String playUrl;

                localSpider = parentController.requireSpider();
                if (localSpider == null) {
                    Platform.runLater(() -> completeReloading(currentSerial));

                    return;
                }
                try {
                    log.debug("playerContent params, flag={}, id={}, vipFlags={}", "", vodId, List.of());
                    resultJson = localSpider.playerContent("", vodId, List.of());
                    result = GsonUtil.fromJson(resultJson, JsonObject.class);
                    log.debug("load playerContent result: {}", result);
                } catch (Exception e) {
                    if (!parentController.tryHandleExecutionInterrupt(e)) {
                        log.error("result exception", e);
                        Platform.runLater(() -> completeReloading(currentSerial));
                    }

                    return;
                }
                if (result == null) {
                    Platform.runLater(() -> completeReloading(currentSerial));

                    return;
                }
                playUrl = getPlayUrl(result);
                Platform.runLater(() -> {
                    playerContentJson = result;
                    playUrlTextField.setText(playUrl);
                    completeReloading(currentSerial, GsonUtil.toPrettyJson(result));
                });
            });
        });

    }

    private void completeReloading() {
        context.postEvent(new AppEvents.SpiderDebuggingViewTabLoaded(SourceAuditType.MOVIE_PLAY, null));
        moviePlayLoadingProperty.set(false);
    }

    private void completeReloading(int serial) {
        completeReloading(serial, null);
    }

    private void completeReloading(int serial, @Nullable String loadedData) {
        if (serial != moviePlayReloadSerial) {

            return;
        }
        context.postEvent(new AppEvents.SpiderDebuggingViewTabLoaded(SourceAuditType.MOVIE_PLAY, loadedData));
        moviePlayLoadingProperty.set(false);
    }

    @Nullable
    private String getPlayUrl(JsonObject result) {
        JsonElement urlElm = result.get("url");

        if (urlElm == null || !urlElm.isJsonPrimitive() || !urlElm.getAsJsonPrimitive().isString()) {

            return null;
        }

        return urlElm.getAsString();
    }

    @Override
    public void clear() {
        player.stop();
        vodIdTextField.setText(null);
        playUrlTextField.setText(null);
        moviePlayLoadingProperty.set(false);
    }

    @Override
    public BooleanProperty getLoadingProperty() {
        return moviePlayLoadingProperty;
    }

    @FXML
    private void onCopyButtonAction(ActionEvent actionEvent) {
        Button copyButton = CastUtil.cast(actionEvent.getSource());
        String text = ((TextInputControl) copyButton.getUserData()).getText();

        if (StringUtils.isEmpty(text)) {

            return;
        }
        ClipboardHelper.setContent(text);
        ToastHelper.showMouseToastI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
    }

    @Override
    public boolean isAutoRefreshOn() {
        return moviePlayTabAutoRefreshCheckBox.isSelected();
    }
}
