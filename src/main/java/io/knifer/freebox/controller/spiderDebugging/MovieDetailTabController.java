package io.knifer.freebox.controller.spiderDebugging;

import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.controller.SpiderDebuggingController;
import io.knifer.freebox.helper.ClipboardHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.common.catvod.Result;
import io.knifer.freebox.model.common.tvbox.AbsXml;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.net.websocket.converter.CatVodBeanConverter;
import io.knifer.freebox.spider.js.JSSpider;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.json.GsonUtil;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 爬虫调试 - 影视详情
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public class MovieDetailTabController extends SpiderDebuggingTabController {

    @FXML
    @Getter
    private CheckBox movieDetailAutoRefreshCheckBox;
    @FXML
    @Getter
    private TextField movieIdTextField;
    @FXML
    private Button movieDetailReloadButton;
    @FXML
    private TextField nameTextField;
    @FXML
    private TextField yearTextField;
    @FXML
    private TextField areaTextField;
    @FXML
    private TextField typeTextField;
    @FXML
    private TextField directorsTextField;
    @FXML
    private TextField actorsTextField;
    @FXML
    private TextField introTextField;
    @FXML
    private TextField vodIdTextField;
    @FXML
    private ProgressIndicator movieDetailTabMovieLoadingProgressIndicator;
    @FXML
    private TabPane episodesTabPane;

    private Button selectedEpBtn;

    private BooleanProperty movieDetailLoadingProperty;
    private int movieDetailReloadSerial;

    private final Context context;
    private final CatVodBeanConverter beanConverter;

    @FXML
    private void initialize() {
        movieDetailLoadingProperty = new SimpleBooleanProperty(false);

        context.registerEventListener(AppEvents.SpiderDebuggingViewInitialized.class, evt -> {
            SpiderDebuggingController controller = evt.controller();

            applyParentController(controller);
            movieDetailReloadButton.disableProperty().bind(movieDetailLoadingProperty);
            movieDetailTabMovieLoadingProgressIndicator.visibleProperty().bind(movieDetailLoadingProperty);
            episodesTabPane.visibleProperty().bind(movieDetailLoadingProperty.not().and(spiderLoadingProperty.not()));
            parentController.setMovieDetailTabController(this);
        });
    }

    @FXML
    private void onMovieDetailReloadButtonAction() {
        reload();
    }

    @Override
    public void reload() {
        String movieId = movieIdTextField.getText();
        int currentSerial;

        if (StringUtils.isEmpty(movieId)) {
            completeReloading();

            return;
        }
        currentSerial = ++movieDetailReloadSerial;
        movieDetailLoadingProperty.set(true);
        clearMovieDetails();
        spiderPreviewTaskMap.compute(SourceAuditType.MOVIE_DETAIL, (k, spiderPreviewTask) -> {
            if (spiderPreviewTask != null) {
                parentController.cancelSpiderTaskIfNeeded(spiderPreviewTask);
            }

            return spiderPreviewExecutor.submit(() -> {
                JSSpider localSpider = parentController.requireSpider();
                String resultJson;
                Result result;
                AbsXml detailContent;
                Movie movie;
                Movie.Video videoInfo;
                Movie.Video.UrlBean urlBean;
                List<Movie.Video.UrlBean.UrlInfo> urlInfoList;

                if (localSpider == null) {
                    Platform.runLater(() -> completeReloading(currentSerial));

                    return;
                }
                try {
                    log.debug("detailContent params, movieId={}", movieId);
                    resultJson = localSpider.detailContent(List.of(movieId));
                    result = GsonUtil.fromJson(resultJson, Result.class);
                    log.debug("load categoryContent result: {}", result);
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
                detailContent = beanConverter.resultToAbsXml(result, BaseValues.DEBUGGING_SOURCE_KEY);
                movie = detailContent.getMovie();
                videoInfo = CollectionUtil.getFirst(movie.getVideoList());
                if (
                        videoInfo == null ||
                        (urlBean = videoInfo.getUrlBean()) == null ||
                        CollectionUtil.isEmpty(urlInfoList = urlBean.getInfoList())
                ) {
                    Platform.runLater(() -> completeReloading(currentSerial));

                    return;
                }
                Platform.runLater(() -> {
                    int year = videoInfo.getYear();
                    ObservableList<Tab> tabs = episodesTabPane.getTabs();

                    // 填充影视基本信息
                    nameTextField.setText(videoInfo.getName());
                    if (year > 0) {
                        yearTextField.setText(String.valueOf(year));
                    }
                    areaTextField.setText(videoInfo.getArea());
                    typeTextField.setText(videoInfo.getType());
                    directorsTextField.setText(videoInfo.getDirector());
                    actorsTextField.setText(videoInfo.getActor());
                    introTextField.setText(videoInfo.getDes());
                    // 填充影视选集信息
                    urlInfoList.forEach(urlInfo -> {
                        String urlFlag = urlInfo.getFlag();
                        Tab tab = new Tab(urlFlag);
                        List<Movie.Video.UrlBean.UrlInfo.InfoBean> beanList = urlInfo.getBeanList();
                        TilePane tilePane = new TilePane();
                        ObservableList<Node> children = tilePane.getChildren();
                        ScrollPane scrollPane = new ScrollPane(tilePane);

                        tilePane.setHgap(10);
                        tilePane.setVgap(10);
                        tilePane.setPadding(new Insets(10));
                        scrollPane.setFitToWidth(true);
                        scrollPane.setFitToHeight(true);
                        beanList.forEach(infoBean -> {
                            String name = infoBean.getName();
                            Button btn = new Button(name);

                            children.add(btn);
                            btn.setTooltip(new Tooltip(name));
                            btn.setFocusTraversable(false);
                            btn.setOnAction(evt -> {
                                if (btn == selectedEpBtn) {

                                    return;
                                }
                                // 选集按钮被点击，更新样式、设置剧集ID
                                updateSelectedEpBtn(btn);
                                vodIdTextField.setText(infoBean.getUrl());
                            });
                        });
                        tab.setContent(scrollPane);
                        tabs.add(tab);
                    });
                    completeReloading(currentSerial, GsonUtil.toPrettyJson(result));
                });
            });
        });
    }

    private void updateSelectedEpBtn(Button newSelectedEpBtn) {
        if (selectedEpBtn != null) {
            selectedEpBtn.getStyleClass().remove("video-details-ep-btn-selected");
        }
        selectedEpBtn = newSelectedEpBtn;
        selectedEpBtn.getStyleClass().add("video-details-ep-btn-selected");
    }

    private void completeReloading() {
        context.postEvent(new AppEvents.SpiderDebuggingViewTabLoaded(SourceAuditType.MOVIE_DETAIL, null));
        movieDetailLoadingProperty.set(false);
    }

    private void completeReloading(int serial) {
        completeReloading(serial, null);
    }

    private void completeReloading(int serial, @Nullable String loadedData) {
        if (serial != movieDetailReloadSerial) {

            return;
        }
        context.postEvent(new AppEvents.SpiderDebuggingViewTabLoaded(SourceAuditType.MOVIE_DETAIL, loadedData));
        movieDetailLoadingProperty.set(false);
    }

    @Override
    public void clear() {
        movieIdTextField.setText(null);
        clearMovieDetails();
        movieDetailLoadingProperty.set(false);
    }

    private void clearMovieDetails() {
        nameTextField.setText(null);
        yearTextField.setText(null);
        areaTextField.setText(null);
        typeTextField.setText(null);
        directorsTextField.setText(null);
        actorsTextField.setText(null);
        introTextField.setText(null);
        vodIdTextField.setText(null);
        episodesTabPane.getTabs().clear();
    }

    @Override
    public BooleanProperty getLoadingProperty() {
        return movieDetailLoadingProperty;
    }

    @FXML
    private void onCopyButtonAction(ActionEvent actionEvent) {
        TextField textField = ((TextField) ((Button) actionEvent.getSource()).getUserData());
        String text = textField.getText();

        if (StringUtils.isEmpty(text)) {

            return;
        }
        ClipboardHelper.setContent(text);
        ToastHelper.showMouseToastI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
    }

    @FXML
    private void onSendToMoviePlayAction() {
        parentController.sendToMoviePlayTab(vodIdTextField.getText());
    }

    @Override
    public boolean isAutoRefreshOn() {
        return movieDetailAutoRefreshCheckBox.isSelected();
    }
}
