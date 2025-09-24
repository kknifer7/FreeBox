package io.knifer.freebox.component.node;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.common.diyp.EPG;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * EPG界面 - 节目块内容弹出框
 *
 * @author Knifer
 */
public class EPGProgramBlockPopOver extends PopOver {

    private final Label startTimeLabel;
    private final Label endTimeLabel;
    private final Label durationLabel;
    private final Label titleLabel;
    private final Label descriptionLabel;

    public EPGProgramBlockPopOver() {
        super();

        VBox root = new VBox();
        ObservableList<Node> rootChildren = root.getChildren();

        startTimeLabel = new Label();
        endTimeLabel = new Label();
        durationLabel = new Label();
        titleLabel = new Label();
        descriptionLabel = new Label();
        startTimeLabel.getStyleClass().add("epg-program-data-list-content");
        endTimeLabel.getStyleClass().add("epg-program-data-list-content");
        durationLabel.getStyleClass().add("epg-program-data-list-content");
        titleLabel.getStyleClass().add("epg-program-data-list-content");
        descriptionLabel.getStyleClass().add("epg-program-data-list-content");
        rootChildren.add(titleLabel);
        rootChildren.add(startTimeLabel);
        rootChildren.add(endTimeLabel);
        rootChildren.add(durationLabel);
        rootChildren.add(descriptionLabel);
        root.getStyleClass().add("epg-program-block-pop-over");
        setContentNode(root);
    }

    public void show(Node owner, EPG.Data epgData) {
        String title = epgData.getTitle();
        String startTimeStr = epgData.getStart();
        String endTimeStr = epgData.getEnd();
        LocalTime startTime = null;
        LocalTime endTime = null;
        String desc;
        Point mouseLocationPoint;

        if (StringUtils.isNotBlank(startTimeStr)) {
            startTimeLabel.setText(I18nHelper.getFormatted(I18nKeys.LIVE_EPG_PROGRAM_START_TIME, startTimeStr));
            try {
                startTime = LocalTime.parse(startTimeStr);
            } catch (DateTimeParseException ignored) {}
        }
        if (StringUtils.isNotBlank(endTimeStr)) {
            endTimeLabel.setText(I18nHelper.getFormatted(I18nKeys.LIVE_EPG_PROGRAM_END_TIME, endTimeStr));
            try {
                endTime = LocalTime.parse(endTimeStr);
            } catch (DateTimeParseException ignored) {}
        }
        if (startTime != null && endTime != null) {
            durationLabel.setText(I18nHelper.getFormatted(
                    I18nKeys.LIVE_EPG_PROGRAM_DURATION,
                    String.valueOf(startTime.until(endTime, ChronoUnit.MINUTES))
            ));
        }
        titleLabel.setText(I18nHelper.getFormatted(I18nKeys.LIVE_EPG_PROGRAM_TITLE, title));
        desc = epgData.getDesc();
        descriptionLabel.setText(I18nHelper.getFormatted(
                I18nKeys.LIVE_EPG_PROGRAM_DESCRIPTION,
                StringUtils.isBlank(desc) ? I18nHelper.get(I18nKeys.LIVE_EPG_NO_EPG_DATA) : desc
        ));
        setTitle(title);
        mouseLocationPoint = MouseInfo.getPointerInfo().getLocation();

        super.show(owner, mouseLocationPoint.getX() + 30, mouseLocationPoint.getY());
    }
}
