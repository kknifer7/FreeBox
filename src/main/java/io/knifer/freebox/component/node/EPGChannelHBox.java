package io.knifer.freebox.component.node;

import io.knifer.freebox.model.common.diyp.EPG;
import io.knifer.freebox.model.domain.LiveChannel;
import io.knifer.freebox.util.CollectionUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.StringJoiner;

/**
 * EPG界面 - 表头频道信息
 *
 * @author Knifer
 */
public class EPGChannelHBox extends HBox {

    private final ProgressIndicator loadingProgressIndicator = new ProgressIndicator();
    private final FontIcon errorIcon = FontIcon.of(FontAwesome.WARNING, 16, Color.ORANGE);
    private final ObjectProperty<List<EPG.Data>> epgDataProperty = new SimpleObjectProperty<>(List.of());
    @Getter
    private final LiveChannel liveChannel;

    public EPGChannelHBox(
            LiveChannel liveChannel,
            EPGChannelProgramDataPopOver showProgramInfoPopOver,
            Runnable onDelete
    ) {
        super(10);

        LogoPane logoPane = new LogoPane(40, 18);
        String channelTitle;
        Label nameLabel;
        Label deleteLabel;
        List<Node> children;

        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("channel-info-box");
        this.liveChannel = liveChannel;
        channelTitle = liveChannel.getTitle();
        logoPane.setTitleAndLogoUrl(
                channelTitle,
                liveChannel.getLines()
                        .stream()
                        .map(LiveChannel.Line::getLogoUrl)
                        .filter(StringUtils::isNotBlank)
                        .findFirst()
                        .orElse(null)
        );
        nameLabel = new Label(channelTitle);
        nameLabel.getStyleClass().add("epg-channel-info-name-label");
        nameLabel.setOnMouseClicked(evt -> {
            Pair<String, String> morningAndAfternoonContent = getFormattedEpgProgramData();

            if (evt.getButton() == MouseButton.PRIMARY && CollectionUtil.isNotEmpty(epgDataProperty.get())) {
                showProgramInfoPopOver.show(
                        nameLabel,
                        channelTitle,
                        morningAndAfternoonContent.getLeft(),
                        morningAndAfternoonContent.getRight()
                );
            }
        });
        deleteLabel = new Label("删除");
        deleteLabel.getStyleClass().add("link-label");
        deleteLabel.setOnMouseClicked(evt -> {
            if (evt.getButton() == MouseButton.PRIMARY) {
                onDelete.run();
            }
        });
        errorIcon.getStyleClass().add("epg-channel-info-error-icon");
        loadingProgressIndicator.setPrefSize(20, 20);
        children = getChildren();
        children.add(loadingProgressIndicator);
        children.add(logoPane);
        children.add(errorIcon);
        children.add(nameLabel);
        children.add(deleteLabel);
        setLoading(false);
        setError(false);
    }

    public void setLoading(boolean loading) {
        loadingProgressIndicator.setVisible(loading);
        loadingProgressIndicator.setManaged(loading);
    }

    public void setError(boolean error) {
        errorIcon.setVisible(error);
        errorIcon.setManaged(error);
    }

    /**
     * 获取节目内容展示文本数据
     * @return 上午的数据-下午的数据
     */
    private Pair<String, String> getFormattedEpgProgramData() {
        List<EPG.Data> epgDataList = epgDataProperty.get();
        String epgDataStartTimeStr;
        StringJoiner stringJoiner;
        boolean findAmFlag;
        boolean findPmFlag;
        String morningContent = null;
        String afternoonContent = null;

        findAmFlag = false;
        findPmFlag = false;
        stringJoiner = new StringJoiner("\n");
        for (EPG.Data epgData : epgDataList) {
            epgDataStartTimeStr = epgData.getStart();
            if (NumberUtils.toInt(StringUtils.substring(epgDataStartTimeStr, 0, 2), 12) < 12 && !findAmFlag) {
                findAmFlag = true;
            }
            if (NumberUtils.toInt(StringUtils.substring(epgDataStartTimeStr, 0, 2), -1) >= 12 && !findPmFlag) {
                if (findAmFlag) {
                    morningContent = stringJoiner.toString();
                    stringJoiner = new StringJoiner("\n");
                }
                findPmFlag = true;
            }
            stringJoiner.add(String.format(
                    "%s-%s %s",
                    epgData.getStart(),
                    epgData.getEnd(),
                    epgData.getTitle()
            ));
        }
        if (findPmFlag) {
            afternoonContent = stringJoiner.toString();
        }

        return Pair.of(morningContent, afternoonContent);
    }

    public void setEpgData(List<EPG.Data> epgData) {
        epgDataProperty.set(epgData);
    }

    public List<EPG.Data> getEpgData() {
        return epgDataProperty.get();
    }
}
