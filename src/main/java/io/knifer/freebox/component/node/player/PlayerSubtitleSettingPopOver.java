package io.knifer.freebox.component.node.player;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.StorageHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.domain.SubtitleInfo;
import io.knifer.freebox.model.domain.SubtitleSearchResponse;
import io.knifer.freebox.service.DownloadService;
import io.knifer.freebox.service.SubtitleFetchService;
import io.knifer.freebox.service.SubtitleSeachService;
import io.knifer.freebox.util.CollectionUtil;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 播放器 - 字幕设置弹出框
 *
 * @author Knifer
 */
public class PlayerSubtitleSettingPopOver extends PopOver {

    /**
     * 字幕延迟变更回调
     * 单位：毫秒 ms
     */
    @Setter
    private Consumer<Long> onSubtitleDelayChanged;
    @Setter
    private Consumer<File> onSubtitleFileChosen;
    private int subtitlePage = 1;
    private int subtitleTotalPage = 1;
    private String searchingKeyword;
    private DownloadService subtitleDownloadService;

    private final SimpleStringProperty movieNameProperty = new SimpleStringProperty();
    private final SimpleLongProperty subtitleDelayProperty = new SimpleLongProperty();
    private final SimpleBooleanProperty loadingProperty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty enableBackwardProperty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty enablePaginatorProperty = new SimpleBooleanProperty(false);

    public PlayerSubtitleSettingPopOver() {
        super();

        Label delayLabel = new Label(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE_DELAY));
        Button delayMinusButton = new Button();
        Label delayValueLabel = new Label();
        Button delayPlusButton = new Button();
        HBox delayHBox = new HBox(delayLabel, delayMinusButton, delayValueLabel, delayPlusButton);
        Button localSelectButton = new Button(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE_LOCAL_SELECT));
        FileChooser localSubtitleFileChooser = new FileChooser();
        Button searchButton = new Button(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE_ONLINE_SEARCH));
        ProgressIndicator loadingProgressIndicator = new ProgressIndicator();
        HBox functionButtonHBox = new HBox(localSelectButton, searchButton, loadingProgressIndicator);
        CheckBox onlyShowHighQualitySubtitleCheckBox = new CheckBox(I18nHelper.get(
                I18nKeys.VIDEO_SETTINGS_SUBTITLE_ONLY_SHOW_HIGH_QUALITY_SUBTITLE
        ));
        Button backwardButton = new Button();
        Button applyButton = new Button();
        HBox operationButtonHBox = new HBox(onlyShowHighQualitySubtitleCheckBox, backwardButton, applyButton);
        BorderPane buttonHBoxAndApplyButtonBorderPane = new BorderPane();
        TextField searchKeywordTextField = new TextField();
        ListView<SubtitleInfo> subtitleListView = new ListView<>();
        List<SubtitleInfo> subtitleInfoArchiveListBackup = new ArrayList<>();
        Pagination subtitlePagination = new Pagination();
        VBox contentVBox = new VBox(
                delayHBox,
                searchKeywordTextField,
                buttonHBoxAndApplyButtonBorderPane,
                subtitleListView,
                subtitlePagination
        );

        SubtitleSeachService searchService = new SubtitleSeachService();
        SubtitleFetchService fetchService = new SubtitleFetchService();

        delayMinusButton.setGraphic(FontIcon.of(FontAwesome.MINUS));
        delayMinusButton.setOnAction(evt -> subtitleDelayProperty.set(subtitleDelayProperty.get() - 100));
        delayValueLabel.getStyleClass().add("royal-blue");
        delayValueLabel.textProperty().bind(subtitleDelayProperty.asString().concat(" ms"));
        delayPlusButton.setGraphic(FontIcon.of(FontAwesome.PLUS));
        delayPlusButton.setOnAction(evt -> subtitleDelayProperty.set(subtitleDelayProperty.get() + 100));
        subtitleDelayProperty.addListener((ob, oldVal, newVal) -> {
            if (newVal != null) {
                onSubtitleDelayChanged.accept(newVal.longValue());
            }
        });
        delayHBox.setSpacing(10);
        delayHBox.setAlignment(Pos.CENTER);
        localSubtitleFileChooser.setTitle(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE_LOCAL_SELECT));
        localSubtitleFileChooser.getExtensionFilters()
                        .add(new FileChooser.ExtensionFilter(
                                I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE_LOCAL_SELECT_EXTENSION_NAME),
                                "*.srt",
                                "*.ass",
                                "*.scc",
                                "*.ssa",
                                "*.ttml",
                                "*.vtt"
                        ));
        localSelectButton.disableProperty().bind(loadingProperty);
        localSelectButton.setOnAction(evt -> {
            File file = localSubtitleFileChooser.showOpenDialog(localSelectButton.getScene().getWindow());

            if (file == null) {

                return;
            }
            onSubtitleFileChosen.accept(file);
            hide();
        });
        movieNameProperty.addListener(
                (ob, oldVal, newVal) -> searchKeywordTextField.setText(newVal)
        );
        subtitleListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<SubtitleInfo> call(ListView<SubtitleInfo> subtitleListView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(SubtitleInfo subtitleInfo, boolean empty) {
                        super.updateItem(subtitleInfo, empty);
                        if (subtitleInfo == null || empty) {
                            setText(null);
                            setGraphic(null);

                            return;
                        }
                        if (subtitleInfo.isArchiveFlag()) {
                            setText(
                                    subtitleInfo.getName() +
                                            I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE_ARCHIVE_PROMPT)
                            );
                        } else{
                            setText(subtitleInfo.getName());
                        }
                        setGraphic(null);
                    }
                };
            }
        });
        subtitlePagination.disableProperty().bind(loadingProperty.or(enablePaginatorProperty.not()));
        subtitlePagination.currentPageIndexProperty()
                .addListener((ob, oldVal, newVal) -> {
                    if (StringUtils.isBlank(searchingKeyword)) {

                        return;
                    }
                    loadingProperty.set(true);
                    enablePaginatorProperty.set(false);
                    subtitlePage = newVal.intValue() + 1;
                    subtitleListView.getItems().clear();
                    searchService.setKeyword(searchingKeyword);
                    searchService.setPage(subtitlePage);
                    searchService.restart();
                });
        searchKeywordTextField.setPromptText(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE_ONLINE_SEARCH_PROMPT));
        searchService.setOnSucceeded(evt -> {
            SubtitleSearchResponse resp = searchService.getValue();
            List<SubtitleInfo> subtitleInfoList = resp.getSubtitleInfoList();
            List<SubtitleInfo> items = subtitleListView.getItems();

            if (CollectionUtil.isEmpty(subtitleInfoList)) {
                loadingProperty.set(false);
                enablePaginatorProperty.set(false);

                return;
            }
            subtitleTotalPage = resp.getTotalPage();
            subtitlePagination.setPageCount(subtitleTotalPage);
            items.addAll(subtitleInfoList);
            loadingProperty.set(false);
            enablePaginatorProperty.set(true);
        });
        fetchService.setOnSucceeded(evt -> {
            List<SubtitleInfo> resultList = fetchService.getValue();

            loadingProperty.set(false);
            if (CollectionUtil.isEmpty(resultList)) {

                return;
            }
            subtitleListView.getItems().addAll(resultList);
        });
        searchButton.disableProperty().bind(loadingProperty);
        searchButton.setGraphic(FontIcon.of(FontAwesome.SEARCH));
        searchButton.setOnAction(evt -> {
            String keyword;

            loadingProperty.set(true);
            enablePaginatorProperty.set(true);
            enableBackwardProperty.set(false);
            subtitleInfoArchiveListBackup.clear();
            keyword = searchKeywordTextField.getText();
            if (StringUtils.isBlank(keyword)) {
                loadingProperty.set(false);

                return;
            }
            searchingKeyword = keyword;
            subtitlePage = 1;
            subtitleTotalPage = 1;
            subtitlePagination.setCurrentPageIndex(0);
            subtitleListView.getItems().clear();
            searchService.setKeyword(keyword);
            searchService.setSortByRank(onlyShowHighQualitySubtitleCheckBox.isSelected());
            searchService.setPage(subtitlePage);
            searchService.restart();
        });
        loadingProgressIndicator.setPrefSize(20, 20);
        loadingProgressIndicator.visibleProperty().bind(loadingProperty);
        functionButtonHBox.setSpacing(5);
        onlyShowHighQualitySubtitleCheckBox.setSelected(true);
        applyButton.disableProperty()
                .bind(loadingProperty.or(subtitleListView.getSelectionModel().selectedItemProperty().isNull()));
        applyButton.setOnAction(evt -> {
            SubtitleInfo subtitleInfo = subtitleListView.getSelectionModel().getSelectedItem();
            ObservableList<SubtitleInfo> subtitleListViewItems = subtitleListView.getItems();
            String url;
            File subtitleFileSavePath;

            if (subtitleInfo == null) {

                return;
            }
            url = subtitleInfo.getUrl();
            if (subtitleInfo.isArchiveFlag()) {
                loadingProperty.set(true);
                enableBackwardProperty.set(true);
                enablePaginatorProperty.set(false);
                subtitleInfoArchiveListBackup.clear();
                subtitleInfoArchiveListBackup.addAll(subtitleListViewItems);
                subtitleListView.getSelectionModel().clearSelection();
                subtitleListViewItems.clear();
                fetchService.setDetailUrl(url);
                fetchService.restart();
            } else if (StringUtils.startsWith(url, "http")) {
                loadingProperty.set(true);
                subtitleFileSavePath = StorageHelper.getTempStoragePath().resolve(subtitleInfo.getName()).toFile();
                if (subtitleDownloadService != null) {
                    subtitleDownloadService.cancel();
                }
                subtitleDownloadService = new DownloadService(
                        url,
                        subtitleFileSavePath,
                        null,
                        null,
                        null
                );
                subtitleDownloadService.setOnFailed(ignored -> {
                    loadingProperty.set(false);
                    ToastHelper.showErrorI18n(I18nKeys.VIDEO_SETTINGS_SUBTITLE_MESSAGE_DOWNLOAD_FAILED);
                });
                subtitleDownloadService.setOnSucceeded(ignored -> {
                    loadingProperty.set(false);
                    if (!subtitleFileSavePath.exists()) {
                        ToastHelper.showErrorI18n(I18nKeys.VIDEO_SETTINGS_SUBTITLE_MESSAGE_DOWNLOAD_FAILED);

                        return;
                    }
                    onSubtitleFileChosen.accept(subtitleFileSavePath);
                    hide();
                });
                subtitleDownloadService.start();
            } else {
                ToastHelper.showErrorI18n(I18nKeys.VIDEO_SETTINGS_SUBTITLE_MESSAGE_DOWNLOAD_FAILED);
            }
        });
        applyButton.setGraphic(FontIcon.of(FontAwesome.CHECK, 17, Color.GREEN));
        backwardButton.disableProperty().bind(loadingProperty.or(enableBackwardProperty.not()));
        backwardButton.setOnAction(evt -> {
            ObservableList<SubtitleInfo> items = subtitleListView.getItems();

            items.clear();
            items.addAll(subtitleInfoArchiveListBackup);
            enablePaginatorProperty.set(true);
            enableBackwardProperty.set(false);
        });
        backwardButton.setGraphic(FontIcon.of(FontAwesome.ARROW_LEFT));
        operationButtonHBox.setAlignment(Pos.CENTER);
        operationButtonHBox.setSpacing(5);
        buttonHBoxAndApplyButtonBorderPane.setLeft(functionButtonHBox);
        buttonHBoxAndApplyButtonBorderPane.setRight(operationButtonHBox);
        contentVBox.setSpacing(5);
        contentVBox.setPadding(new Insets(10));
        contentVBox.setMinWidth(450);
        setContentNode(contentVBox);
        setDetachable(false);
        setOnShowing(evt -> {
            searchingKeyword = null;
            loadingProperty.set(false);
            enableBackwardProperty.set(false);
            enablePaginatorProperty.set(false);
            subtitlePage = 1;
            subtitleTotalPage = 1;
            onlyShowHighQualitySubtitleCheckBox.setSelected(true);
            subtitleInfoArchiveListBackup.clear();
            subtitleListView.getItems().clear();
            subtitleListView.getSelectionModel().clearSelection();
            subtitlePagination.setCurrentPageIndex(0);
            searchKeywordTextField.setText(movieNameProperty.get());
            searchKeywordTextField.requestFocus();
        });
        setOnShown(evt -> setDetached(true));
        setOnHiding(evt -> {
            searchService.cancel();
            fetchService.cancel();
            if (subtitleDownloadService != null) {
                subtitleDownloadService.cancel();
            }
        });
        setOnCloseRequest(evt -> {
            searchService.cancel();
            fetchService.cancel();
            if (subtitleDownloadService != null) {
                subtitleDownloadService.cancel();
            }
        });
        setTitle(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE));
    }

    public void setMovieName(String movieName) {
        this.movieNameProperty.set(movieName);
    }

    public void resetSubtitleDelay() {
        this.subtitleDelayProperty.set(0);
    }
}
