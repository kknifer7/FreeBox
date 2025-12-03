package io.knifer.freebox.component.node.player;

import io.knifer.freebox.model.domain.LiveChannel;
import io.knifer.freebox.util.CollectionUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 播放器（直播模式） - 频道线路分页
 *
 * @author Knifer
 */
public class PlayerLiveChannelLinesWithPaginator extends HBox {

    private LiveChannel.Line playingLiveChannelLine;

    private final SimpleIntegerProperty currentPage = new SimpleIntegerProperty(-1);
    private final SimpleIntegerProperty totalPage = new SimpleIntegerProperty(-1);
    private final HBox linesHBox = new HBox(8);
    private final Consumer<LiveChannel.Line> onLiveChannelLineChanged;
    private final List<PlayerLiveChannelLineLabel> lineLabels;

    private final static int PAGE_SIZE = 5;

    public PlayerLiveChannelLinesWithPaginator(Consumer<LiveChannel.Line> onLiveChannelLineChanged) {
        super(8);

        Label previousLabel = new Label();
        Label nextLabel = new Label();
        List<Node> children;

        this.onLiveChannelLineChanged = onLiveChannelLineChanged;
        this.lineLabels = new ArrayList<>();
        previousLabel.setGraphic(FontIcon.of(FontAwesome.CARET_LEFT, 32, Color.WHITE));
        previousLabel.setCursor(Cursor.HAND);
        nextLabel.setGraphic(FontIcon.of(FontAwesome.CARET_RIGHT, 32, Color.WHITE));
        nextLabel.setCursor(Cursor.HAND);
        previousLabel.visibleProperty().bind(currentPage.greaterThan(1));
        nextLabel.visibleProperty().bind(currentPage.lessThan(totalPage));
        previousLabel.setOnMouseClicked(evt -> {
            int nowCurrentPage;
            List<Node> linesHBoxChildren;

            if (evt.getButton() != MouseButton.PRIMARY || currentPage.get() <= 1) {

                return;
            }
            nowCurrentPage = currentPage.get() - 1;
            currentPage.set(nowCurrentPage);
            linesHBoxChildren = linesHBox.getChildren();
            linesHBoxChildren.clear();
            for (int i = (nowCurrentPage - 1) * PAGE_SIZE; i < nowCurrentPage * PAGE_SIZE; i++) {
                if (i >= lineLabels.size()) {
                    break;
                }
                linesHBoxChildren.add(lineLabels.get(i));
            }
        });
        nextLabel.setOnMouseClicked(evt -> {
            int nowCurrentPage;
            List<Node> linesHBoxChildren;

            if (evt.getButton() != MouseButton.PRIMARY || currentPage.get() >= totalPage.get()) {

                return;
            }
            nowCurrentPage = currentPage.get() + 1;
            currentPage.set(nowCurrentPage);
            linesHBoxChildren = linesHBox.getChildren();
            linesHBoxChildren.clear();
            for (int i = (nowCurrentPage - 1) * PAGE_SIZE; i < nowCurrentPage * PAGE_SIZE; i++) {
                if (i >= lineLabels.size()) {
                    break;
                }
                linesHBoxChildren.add(lineLabels.get(i));
            }
        });
        children = getChildren();
        children.add(previousLabel);
        children.add(linesHBox);
        children.add(nextLabel);
    }

    public void clear() {
        linesHBox.getChildren().clear();
        lineLabels.clear();
        currentPage.set(-1);
        totalPage.set(-1);
        playingLiveChannelLine = null;
    }

    public void addLine(LiveChannel.Line line) {
        PlayerLiveChannelLineLabel newLineLabel = new PlayerLiveChannelLineLabel(line);
        ObservableList<Node> linesHBoxChildren = linesHBox.getChildren();
        PlayerLiveChannelLineLabel firstLineLabel;

        newLineLabel.setOnMouseClicked(evt -> {
            if (evt.getButton() != MouseButton.PRIMARY || line == playingLiveChannelLine) {

                return;
            }
            for (PlayerLiveChannelLineLabel lineLabel : lineLabels) {
                // 为正在播放的线路标签添加样式，移除其他线路标签的样式
                setLineLabelStyle(
                        lineLabel,
                        playingLiveChannelLine == null || lineLabel.getLiveChannelLine() == line
                );
            }
            playingLiveChannelLine = line;
            onLiveChannelLineChanged.accept(line);
        });
        lineLabels.add(newLineLabel);
        if (currentPage.get() == -1) {
            currentPage.set(1);
        }
        if (totalPage.get() == -1) {
            totalPage.set(1);
        } else {
            totalPage.set((lineLabels.size() - 1) / PAGE_SIZE + 1);
        }
        if (lineLabels.size() <= PAGE_SIZE) {
            linesHBoxChildren.add(newLineLabel);
        }
        // 默认将第一个线路标签设为正在播放
        firstLineLabel = (PlayerLiveChannelLineLabel) CollectionUtil.getFirst(linesHBoxChildren);
        if (firstLineLabel != null && playingLiveChannelLine == null) {
            setLineLabelStyle(firstLineLabel, true);
            playingLiveChannelLine = firstLineLabel.getLiveChannelLine();
        }
    }

    /**
     * 设置线路聚焦
     * @param line 线路
     */
    public void focus(LiveChannel.Line line) {
        LiveChannel.Line lineLabelLine;
        PlayerLiveChannelLineLabel focusLabel = null;
        PlayerLiveChannelLineLabel unFocusLabel = null;

        for (PlayerLiveChannelLineLabel lineLabel : lineLabels) {
            lineLabelLine = lineLabel.getLiveChannelLine();
            if (lineLabelLine.equals(line)) {
                focusLabel = lineLabel;
                if (playingLiveChannelLine == null) {
                    break;
                }
            } else if (lineLabelLine.equals(playingLiveChannelLine)) {
                unFocusLabel = lineLabel;
            }
        }
        if (focusLabel == null) {

            return;
        } else {
            setLineLabelStyle(focusLabel, true);
        }
        if (unFocusLabel != null) {
            setLineLabelStyle(unFocusLabel, false);
        }
        playingLiveChannelLine = line;
    }

    private void setLineLabelStyle(PlayerLiveChannelLineLabel lineLabel, boolean focused) {
        List<String> styleClasses = lineLabel.getStyleClass();

        if (focused) {
            styleClasses.remove("player-live-channel-line-label");
            if (!styleClasses.contains("player-live-channel-line-label-focused")) {
                styleClasses.add("player-live-channel-line-label-focused");
            }
        } else {
            styleClasses.remove("player-live-channel-line-label-focused");
            if (!styleClasses.contains("player-live-channel-line-label")) {
                styleClasses.add("player-live-channel-line-label");
            }
        }
    }
}
