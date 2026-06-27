package io.knifer.freebox.controller.spiderDebugging;

import io.knifer.freebox.component.factory.VideoGridCellFactory;
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
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.NodeUtil;
import io.knifer.freebox.util.json.GsonUtil;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.GridView;
import org.controlsfx.control.InfoOverlay;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 爬虫调试 - 影视搜索
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public class MovieSearchTabController extends SpiderDebuggingTabController {

    @FXML
    private CheckBox movieSearchAutoRefreshCheckBox;
    @FXML
    private TextField searchKeywordTextField;
    @FXML
    private TextField movieIdTextField;
    @FXML
    private ProgressIndicator movieSearchLoadingProgressIndicator;
    @FXML
    private GridView<Movie.Video> movieSearchGridView;

    private VideoGridCellFactory videoGridCellFactory;

    private InfoOverlay lastSelectedExploreMovieInfoOverlay;

    private BooleanProperty movieSearchLoadingProperty;
    private int movieSearchReloadSerial;

    private final Context context;
    private final CatVodBeanConverter beanConverter;

    @FXML
    private void initialize() {
        movieSearchLoadingProperty = new SimpleBooleanProperty(false);
        videoGridCellFactory = new VideoGridCellFactory(this::selectSearchMovie, cell -> {});
        movieSearchGridView.setCellFactory(videoGridCellFactory);
        context.registerEventListener(
                AppEvents.SpiderDebuggingViewInitialized.class,
                evt -> {
                    SpiderDebuggingController controller = evt.controller();

                    applyParentController(controller);
                    movieSearchLoadingProgressIndicator.visibleProperty()
                            .bind(movieSearchLoadingProperty.or(spiderLoadingProperty));
                    controller.setMovieSearchTabController(this);
                }
        );
    }

    private void selectSearchMovie(Movie.Video movie) {
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
    }

    @FXML
    private void onSearchButtonAction() {
        reload();
    }

    @FXML
    private void onCopyButtonAction(ActionEvent actionEvent) {
        Button copyButton = CastUtil.cast(actionEvent.getSource());
        TextInputControl node = CastUtil.cast(copyButton.getUserData());
        String text = node.getText();

        if (StringUtils.isEmpty(text)) {

            return;
        }
        ClipboardHelper.setContent(text);
        ToastHelper.showMouseToastI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
    }

    @FXML
    private void onSendToMovieDetailButtonAction() {
        parentController.sendToMovieDetailTab(movieIdTextField.getText());
    }

    @Override
    public void reload() {
        String keyword = searchKeywordTextField.getText();
        int currentSerial;

        if (StringUtils.isEmpty(keyword)) {

            return;
        }
        currentSerial = ++movieSearchReloadSerial;
        movieSearchLoadingProperty.set(true);
        movieSearchGridView.getItems().clear();
        spiderPreviewTaskMap.compute(SourceAuditType.MOVIE_SEARCH, (k, spiderPreviewTask) -> {
            if (spiderPreviewTask != null) {
                parentController.cancelSpiderTaskIfNeeded(spiderPreviewTask);
            }

            return spiderPreviewExecutor.submit(() -> {
                JSSpider localSpider = parentController.requireSpider();
                String resultStr;
                Result result;
                AbsXml searchContent;
                Movie movieInfo;
                List<Movie.Video> videoList;

                if (localSpider == null) {
                    Platform.runLater(() -> completeReloading(currentSerial));

                    return;
                }
                try {
                    log.debug("movie search, keyword={}", keyword);
                    resultStr = localSpider.searchContent(keyword, false, "1");
                    log.debug("movie search result: {}", resultStr);
                    result = GsonUtil.fromJson(resultStr, Result.class);
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
                searchContent = beanConverter.resultToAbsXml(result, BaseValues.DEBUGGING_SOURCE_KEY);
                movieInfo = searchContent.getMovie();
                if (movieInfo == null || CollectionUtil.isEmpty(videoList = movieInfo.getVideoList())) {
                    Platform.runLater(() -> completeReloading(currentSerial));

                    return;
                }
                Platform.runLater(() -> {
                    movieSearchGridView.getItems().addAll(videoList);
                    completeReloading(currentSerial, GsonUtil.toPrettyJson(result));
                });
            });
        });
    }

    private void completeReloading(int serial) {
        completeReloading(serial, null);
    }

    private void completeReloading(int serial, @Nullable String resultJson) {
        if (serial != movieSearchReloadSerial) {

            return;
        }
        context.postEvent(new AppEvents.SpiderDebuggingViewTabLoaded(SourceAuditType.MOVIE_SEARCH, resultJson));
        movieSearchLoadingProperty.set(false);
    }

    @Override
    public void clear() {
        searchKeywordTextField.clear();
        movieIdTextField.clear();
    }

    @Override
    public BooleanProperty getLoadingProperty() {
        return movieSearchLoadingProperty;
    }

    @FXML
    private void onSearchKeywordTextFieldKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            onSearchButtonAction();
        }
    }

    @Override
    public boolean isAutoRefreshOn() {
        return movieSearchAutoRefreshCheckBox.isSelected();
    }
}
