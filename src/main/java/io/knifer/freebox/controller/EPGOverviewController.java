package io.knifer.freebox.controller;

import cn.hutool.core.map.multi.RowKeyTable;
import cn.hutool.core.map.multi.Table;
import io.knifer.freebox.component.node.EPGChannelHBox;
import io.knifer.freebox.component.node.EPGChannelProgramDataPopOver;
import io.knifer.freebox.component.node.EPGProgramBlockPane;
import io.knifer.freebox.component.node.EPGProgramBlockPopOver;
import io.knifer.freebox.constant.EPGTimelineMode;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.handler.impl.ParameterizedEpgFetchingHandler;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.bo.EPGOverviewBO;
import io.knifer.freebox.model.common.diyp.EPG;
import io.knifer.freebox.model.domain.LiveChannel;
import io.knifer.freebox.model.domain.LiveChannelGroup;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CollectionUtil;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.textfield.CustomTextField;

import javax.annotation.Nullable;
import javax.swing.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * EPG概览
 *
 * @author Knifer
 */
@Slf4j
public class EPGOverviewController extends BaseController implements Destroyable {

    @FXML
    private VBox root;
    @FXML
    private Label dateLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private SearchableComboBox<Pair<String, LiveChannel>> searchAddChannelComboBox;
    @FXML
    private HBox controlBarHBox;
    @FXML
    private SegmentedButton timelineChangeSegmentedButton;
    @FXML
    private ScrollPane channelInfoScrollPane;
    @FXML
    private ScrollPane timelineScrollPane;
    @FXML
    private Label timelineStartLabel;
    @FXML
    private Label timelineEndLabel;
    @FXML
    private ScrollPane programScrollPane;
    @FXML
    private VBox channelInfoVBox;
    @FXML
    private VBox programBlockVBox;

    private EPGChannelProgramDataPopOver epgDataViewPopOver;
    private EPGProgramBlockPopOver epgProgramBlockPopOver;

    private LocalDateTime now;
    private Timer datetimeUpdateTimer;
    private Set<LiveChannel> addedChannels;
    private ParameterizedEpgFetchingHandler epgFetchingHandler;
    private List<LiveChannelGroup> liveChannelGroups;
    private String epgServiceUrl;
    private Table<LocalDate, LiveChannel, EPG> epgDataTable;

    private EPGTimelineMode timelineMode = EPGTimelineMode.COMMON;

    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE");
    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        ReadOnlyDoubleProperty rootWidthProp;
        DoubleBinding timelineScrollPaneWidthPropBinding;
        ReadOnlyDoubleProperty timelineScrollPaneHeightProp;
        DoubleBinding searchAddChannelComboBoxWidthProp;

        epgFetchingHandler = ParameterizedEpgFetchingHandler.getInstance();
        epgDataTable = new RowKeyTable<>();
        addedChannels = new HashSet<>();
        now = LocalDateTime.now();

        datetimeUpdateTimer = new Timer(1000, evt -> {
            now = now.plusSeconds(1);
            Platform.runLater(() -> updateDatetimeLabel(now));
            if (now.getHour() == 0 && now.getMinute() == 0) {
                loadEpgForAllAddedChannel(now.toLocalDate());
            }
        });
        updateDatetimeLabel(now);
        datetimeUpdateTimer.start();

        rootWidthProp = root.widthProperty();
        timelineScrollPaneWidthPropBinding = rootWidthProp.subtract(channelInfoScrollPane.widthProperty());
        timelineScrollPane.minWidthProperty().bind(timelineScrollPaneWidthPropBinding);
        timelineScrollPane.maxWidthProperty().bind(timelineScrollPaneWidthPropBinding);
        timelineScrollPane.addEventFilter(ScrollEvent.SCROLL, evt -> {
            if (evt.getDeltaY() != 0) {
                // 已经禁止了垂直滚动条，但还不够，用户可以通过鼠标滚轮实现滚动，所以还需要通过这种方式吃掉垂直滚动事件
                evt.consume();
            }
        });
        timelineScrollPaneHeightProp = timelineScrollPane.heightProperty();
        programScrollPane.minHeightProperty().bind(timelineScrollPaneHeightProp);
        programScrollPane.maxHeightProperty().bind(timelineScrollPaneHeightProp);
        channelInfoScrollPane.minWidthProperty().bind(rootWidthProp.divide(5));

        timelineStartLabel.setText(timelineMode.getStartTime().format(TIME_FORMATTER));
        timelineEndLabel.setText(timelineMode.getEndTime().format(TIME_FORMATTER));
        timelineChangeSegmentedButton.getToggleGroup()
                .selectedToggleProperty()
                .addListener((ob, oldVal, newVal) -> {
                    if (newVal == null) {

                        return;
                    }
                    // 处理时间轴改变
                    timelineMode = EPGTimelineMode.getByValue((String) newVal.getUserData());
                    if (timelineMode == null) {

                        return;
                    }
                    timelineStartLabel.setText(timelineMode.getStartTime().format(TIME_FORMATTER));
                    timelineEndLabel.setText(timelineMode.getEndTime().format(TIME_FORMATTER));
                    // 因为改变了时间轴，所以要重新加载所有节目块
                    refreshProgramBlocks();
                    // 将滚动条拉到最开始
                    timelineScrollPane.setHvalue(0);
                });

        epgDataViewPopOver = new EPGChannelProgramDataPopOver();
        epgProgramBlockPopOver = new EPGProgramBlockPopOver();

        searchAddChannelComboBoxWidthProp = controlBarHBox.widthProperty().divide(5);
        searchAddChannelComboBox.minWidthProperty().bind(searchAddChannelComboBoxWidthProp);
        searchAddChannelComboBox.maxWidthProperty().bind(searchAddChannelComboBoxWidthProp);
        searchAddChannelComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Pair<String, LiveChannel> groupTitleAndLiveChannel) {
                return groupTitleAndLiveChannel.getRight().getTitle() + " - " + groupTitleAndLiveChannel.getLeft();
            }

            @Override
            public Pair<String, LiveChannel> fromString(String s) {
                String[] groupTitleAndChannelTitle = s.split(" - ", 2);

                return searchAddChannelComboBox.getItems().stream()
                        .filter(
                                pair ->
                                        pair.getRight().getTitle().equals(groupTitleAndChannelTitle[1]) &&
                                                pair.getLeft().equals(groupTitleAndChannelTitle[0])
                        )
                        .findFirst()
                        .orElse(null);
            }
        });
        searchAddChannelComboBox.valueProperty()
                .addListener((
                        ob,
                        oldVal,
                        newVal
                ) -> {
                    LiveChannel liveChannel;

                    if (
                            newVal == null ||
                            (liveChannel = newVal.getRight()) == null ||
                            addedChannels.contains(liveChannel)
                    ) {

                        return;
                    }
                    addLiveChannel(liveChannel, now.toLocalDate());
                    searchAddChannelComboBox.setValue(null);
                });

        Platform.runLater(() -> {
            ObservableList<Pair<String, LiveChannel>> searchAddChannelComboBoxItems;
            Stage stage;
            EPGOverviewBO bo;
            LiveChannel defaultLiveChannel;

            bo = getData();
            liveChannelGroups = bo.getLiveChannelGroups();
            epgServiceUrl = bo.getEpgServiceUrl();
            defaultLiveChannel = bo.getDefaultLiveChannel();
            searchAddChannelComboBox.getChildrenUnmodifiable()
                    .stream()
                    .filter(node -> node instanceof CustomTextField)
                    .findFirst()
                    .ifPresent(node ->
                            ((CustomTextField) node).setPromptText(I18nHelper.get(I18nKeys.LIVE_EPG_SEARCH_ADD_HINT))
                    );
            if (CollectionUtil.isNotEmpty(liveChannelGroups)) {
                searchAddChannelComboBoxItems = searchAddChannelComboBox.getItems();
                liveChannelGroups.stream()
                        .map(group -> Pair.of(group.getTitle(), group.getChannels()))
                        .flatMap(
                                pair -> pair.getValue()
                                        .stream()
                                        .map(channel -> Pair.of(pair.getLeft(), channel))
                        )
                        .forEach(searchAddChannelComboBoxItems::add);
            }
            stage = WindowHelper.getStage(root);
            stage.widthProperty().addListener(
                    (ob, oldVal, newVal) -> refreshProgramBlocks()
            );
            stage.setOnCloseRequest(evt -> destroy());

            // 数据加载
            if (defaultLiveChannel != null) {
                addLiveChannel(defaultLiveChannel, now.toLocalDate());
            }
        });
    }

    private void updateDatetimeLabel(LocalDateTime datetime) {
        String newDate = DATE_FORMATTER.format(datetime);
        String newTime = TIME_FORMATTER.format(datetime);

        if (!newDate.equals(dateLabel.getText())) {
            dateLabel.setText(newDate);
        }
        if (!newTime.equals(timeLabel.getText())) {
            timeLabel.setText(newTime);
        }
    }

    @Override
    public void destroy() {
        datetimeUpdateTimer.stop();
        epgDataTable.clear();
        addedChannels.clear();
    }

    private void addLiveChannel(LiveChannel liveChannel, LocalDate date) {
        EPGChannelHBox channelHBox;
        ObservableList<Node> channelInfoVBoxChildren;
        int idx;
        EPG epg;

        if (addedChannels.contains(liveChannel)) {
            ToastHelper.showInfoI18n(I18nKeys.LIVE_EPG_MESSAGE_LIVE_CHANNEL_ALREADY_ADDED);

            return;
        }
        addedChannels.add(liveChannel);
        channelHBox = new EPGChannelHBox(
                liveChannel,
                epgDataViewPopOver,
                () -> {
                    removeEPGNodesByLiveChannel(liveChannel);
                    addedChannels.remove(liveChannel);
                }
        );
        channelInfoVBoxChildren = channelInfoVBox.getChildren();
        channelInfoVBoxChildren.add(channelHBox);
        idx = channelInfoVBoxChildren.size() - 1;
        epg = epgDataTable.get(date, liveChannel);

        if (epg == null) {
            loadEpgByLiveChannel(date, channelHBox, liveChannel, idx);
        } else {
            channelHBox.setEpgData(epg.getEpgData());
            fillEPGChannelHBox(idx, channelHBox);
        }

    }

    private void fetchEpg(
            LiveChannel liveChannel,
            LocalDate date,
            Consumer<EPG> successCallback,
            Runnable failCallback,
            Consumer<Throwable> errorCallback
    ) {
        AsyncUtil.execute(() -> {
            EPG epg = epgDataTable.get(date, liveChannel);

            if (epg == null) {
                try {
                    epg = epgFetchingHandler.handle(
                            epgServiceUrl, liveChannel.getTitle(), date
                    ).get(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    errorCallback.accept(e);

                    return;
                } catch (ExecutionException | TimeoutException e) {
                    log.warn("epg loading timeout: {}", liveChannel, e);
                    failCallback.run();

                    return;
                }
                if (epg == null || CollectionUtil.isEmpty(epg.getEpgData())) {
                    log.warn("epg data invalid, channel={}, epg={}", liveChannel, epg);
                    failCallback.run();

                    return;
                }
            }
            successCallback.accept(epg);
        });
    }

    private void fillEPGChannelHBox(int idx, EPGChannelHBox channelHBox) {
        ObservableList<Node> programBlockVBoxChildren = programBlockVBox.getChildren();
        HBox programBlockHBox;
        EPG.Data lastEpgData;
        List<EPG.Data> epgDataList;
        Pair<Double, Double> programBlockLeftMarginAndWidth;
        EPGProgramBlockPane programBlockPane;

        programBlockVBoxChildren.removeIf(node -> {
            // 删除旧的节目块
            if (node instanceof HBox blockHBox) {

                return blockHBox.getUserData() == channelHBox;
            }

            return false;
        });
        programBlockHBox = new HBox();
        programBlockHBox.minHeightProperty().bind(channelHBox.heightProperty());
        programBlockHBox.maxHeightProperty().bind(channelHBox.heightProperty());
        // 为了能关联节目块box和左侧频道box，设置一个userData
        programBlockHBox.setUserData(channelHBox);
        lastEpgData = null;
        epgDataList = channelHBox.getEpgData();
        for (EPG.Data programData : epgDataList) {
            programBlockLeftMarginAndWidth = calculateProgramBlockLeftMarginAndWidth(
                    lastEpgData == null ? null : lastEpgData.getEnd(),
                    programData,
                    timelineMode.getStartTime(),
                    timelineMode.getEndTime()
            );
            if (programBlockLeftMarginAndWidth == null) {
                continue;
            }
            programBlockPane = new EPGProgramBlockPane(programData);
            EPGProgramBlockPane finalProgramBlockPane = programBlockPane;
            programBlockPane.setOnMousePressed(evt -> {
                if (evt.getButton() != MouseButton.PRIMARY) {

                    return;
                }
                epgProgramBlockPopOver.show(finalProgramBlockPane, programData);
            });
            HBox.setMargin(
                    programBlockPane,
                    new Insets(0, 0, 0, programBlockLeftMarginAndWidth.getLeft())
            );
            programBlockPane.setPrefWidth(programBlockLeftMarginAndWidth.getValue());
            programBlockPane.setMinWidth(programBlockLeftMarginAndWidth.getValue());
            programBlockPane.setMaxWidth(programBlockLeftMarginAndWidth.getValue());
            programBlockHBox.maxHeightProperty().bind(channelHBox.heightProperty());
            programBlockHBox.getChildren().add(programBlockPane);
            lastEpgData = programData;
        }
        programBlockVBoxChildren.add(idx, programBlockHBox);
        channelHBox.setLoading(false);
    }

    private void removeEPGNodesByLiveChannel(LiveChannel liveChannel) {
        ObservableList<Node> programBlockVBoxChildren = programBlockVBox.getChildren();
        EPGChannelHBox epgChannelHBox = null;

        for (Node item : channelInfoVBox.getChildren()) {
            if (item instanceof EPGChannelHBox channelHBox) {
                if (channelHBox.getLiveChannel() == liveChannel) {
                    channelInfoVBox.getChildren().remove(item);
                    epgChannelHBox = channelHBox;
                    break;
                }
            }
        }
        if (epgChannelHBox == null) {

            return;
        }
        for (Node item : programBlockVBoxChildren) {
            if (item instanceof HBox blockHBox) {
                if (blockHBox.getUserData() == epgChannelHBox) {
                    programBlockVBoxChildren.remove(item);
                    break;
                }
            }
        }
    }

    private void refreshProgramBlocks() {
        ObservableList<Node> programBlockVBoxChildren = programBlockVBox.getChildren();
        ObservableList<Node> channelInfoVBoxChildren;
        Node epgChannelHBoxNode;
        EPGChannelHBox epgChannelHBox;

        // 按照当前列表中所有的频道，重新加载（创建）所有节目块组件
        programBlockVBoxChildren.clear();
        channelInfoVBoxChildren = channelInfoVBox.getChildren();
        for (int i = 0; i < channelInfoVBoxChildren.size(); i++) {
            epgChannelHBoxNode = channelInfoVBoxChildren.get(i);
            epgChannelHBox = (EPGChannelHBox) epgChannelHBoxNode;
            fillEPGChannelHBox(i, epgChannelHBox);
        }
    }

    /**
     * 根据上一个节目结束的时间点，计算出节目块的左边距和宽度
     * @param lastProgramEndTimeStr 上一个节目结束的时间点（eg. 12:00）
     * @param programData EPG节目数据
     * @param timelineStartTime 时间轴的开始时间
     * @param timelineEndTime 时间轴的结束时间
     * @return 节目的左边距和宽度，如果返回null，则表示无需添加该节目块
     */
    @Nullable
    private Pair<Double, Double> calculateProgramBlockLeftMarginAndWidth(
            @Nullable String lastProgramEndTimeStr,
            EPG.Data programData,
            LocalTime timelineStartTime,
            LocalTime timelineEndTime
    ) {
        String start = programData.getStart();
        String end = programData.getEnd();
        LocalTime startTime;
        LocalTime endTime;
        double oneMinuteLength;
        LocalTime lastProgramEndTime;

        if (start == null || end == null) {
            log.warn("epg time is null, programData={}", programData);

            return null;
        }
        try {
            startTime = LocalTime.parse(start);
            endTime = LocalTime.parse(programData.getEnd());
        } catch (DateTimeParseException e) {
            log.warn("Invalid epg time format, programData={}", programData);

            return null;
        }
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            log.warn("epg time range is invalid, programData={}", programData);

            return null;
        }
        oneMinuteLength = WindowHelper.getStage(timelineScrollPane).getWidth() / 300;
        if (!endTime.isAfter(timelineStartTime) || !startTime.isBefore(timelineEndTime)) {
            // 节目结束时间点不在时间轴开头，或开始时间点不在时间轴结尾，无需添加该节目

            return null;
        }
        if (startTime.isBefore(timelineStartTime)) {
            // 节目开始的时间点在时间轴之前，则将开始时间设置为时间轴起点
            startTime = timelineStartTime;
        }
        if (endTime.isAfter(timelineEndTime)) {
            // 节目结束时间点在时间轴之后，则将结束时间设置为时间轴终点
            endTime = timelineEndTime;
        }
        if (lastProgramEndTimeStr == null) {
            // 设置首个节目时，没有上个节目的结束时间，将上个节目结束时间设置为时间轴起点
            lastProgramEndTime = timelineStartTime;
        } else {
            lastProgramEndTime = LocalTime.parse(lastProgramEndTimeStr);
        }

        return Pair.of(
                lastProgramEndTime.until(startTime, ChronoUnit.MINUTES) * oneMinuteLength,
                startTime.until(endTime, ChronoUnit.MINUTES) * oneMinuteLength
        );
    }

    @FXML
    private void onSwitchDateButtonAction(ActionEvent event) {
        Button actionBtn = ((Button) event.getSource());
        String userData = (String) actionBtn.getUserData();

        datetimeUpdateTimer.stop();
        if (userData.equals("0")) {
            now = LocalDateTime.now();
        } else {
            now = now.plusDays(Integer.parseInt(userData));
        }
        updateDatetimeLabel(now);
        datetimeUpdateTimer.restart();
        loadEpgForAllAddedChannel(now.toLocalDate());
    }

    private void loadEpgForAllAddedChannel(LocalDate date) {
        ObservableList<Node> channelInfoVBoxChildren = channelInfoVBox.getChildren();
        Node channelHBoxNode;

        for (int i = 0; i < channelInfoVBoxChildren.size(); i++) {
            channelHBoxNode = channelInfoVBoxChildren.get(i);
            if (channelHBoxNode instanceof EPGChannelHBox channelHBox) {
                loadEpgByLiveChannel(date, channelHBox, channelHBox.getLiveChannel(), i);
            }
        }
    }

    private void loadEpgByLiveChannel(LocalDate date, EPGChannelHBox channelHBox, LiveChannel channel, int idx) {
        if (StringUtils.isBlank(epgServiceUrl)) {

            return;
        }
        channelHBox.setLoading(true);
        channelHBox.setError(false);
        fetchEpg(
                channel,
                date,
                (fetchedEpg) -> Platform.runLater(() -> {
                    epgDataTable.put(date, channel, fetchedEpg);
                    channelHBox.setEpgData(fetchedEpg.getEpgData());
                    fillEPGChannelHBox(idx, channelHBox);
                    channelHBox.setLoading(false);
                }),
                () -> Platform.runLater(() -> {
                    channelHBox.setLoading(false);
                    channelHBox.setError(true);
                }),
                (e) -> Platform.runLater(() -> {
                        channelHBox.setLoading(false);
                        channelHBox.setError(true);
                        ToastHelper.showException(e);
                })
        );
    }
}
