package io.knifer.freebox.component.node;

import io.knifer.freebox.model.common.diyp.EPG;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * EPG界面 - 表中节目块
 *
 * @author Knifer
 */
@Getter
public class EPGProgramBlockPane extends StackPane {

    private final EPG.Data programData;

    public EPGProgramBlockPane(EPG.Data programData) {
        super();
        this.programData = programData;
        getStyleClass().add("epg-program-block-pane");

        VBox programInfo = new VBox(5);
        ObservableList<Node> programInfoChildren = programInfo.getChildren();
        String programTitle = programData.getTitle();
        String programTimeRange = programData.getStart() + " ~ " + programData.getEnd();
        String programInfoTooltipText = programTitle + "\n\n" + programTimeRange;
        Tooltip programInfoTooltip = new Tooltip(programInfoTooltipText);
        Label titleLabel = new Label(programTitle);
        Label timeLabel = new Label(programTimeRange);
        FontIcon clockIcon = FontIcon.of(FontAwesome.CLOCK_O);

        programInfo.setAlignment(Pos.TOP_LEFT);
        programInfoTooltip.getStyleClass().add("epg-program-info-tooltip");
        Tooltip.install(programInfo, programInfoTooltip);
        titleLabel.getStyleClass().add("epg-program-title-label");
        timeLabel.getStyleClass().add("epg-program-time-label");
        clockIcon.getStyleClass().add("epg-program-time-label-icon");
        timeLabel.setGraphic(clockIcon);
        programInfoChildren.add(titleLabel);
        programInfoChildren.add(timeLabel);
        getChildren().add(programInfo);
    }
}
