package io.knifer.freebox.component.node.player;

import io.knifer.freebox.controller.Destroyable;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.PopOver;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.swing.*;

/**
 * 播放器 - 附着弹出框的控件标签
 *
 * @author Knifer
 */
@Slf4j
public class PlayerPopOverControlLabel extends Label implements Destroyable {

    private final Timer popOverHideTimer;

    public PlayerPopOverControlLabel(FontIcon icon, Pane popOverContentPane) {
        super();

        PopOver popOver = new PopOver(popOverContentPane);

        popOverHideTimer = new Timer(1000, evt -> popOver.hide());
        popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_LEFT);
        popOver.getStyleClass().add("player-pop-over");
        popOver.setDetachable(false);
        addEventFilter(MouseEvent.MOUSE_ENTERED, event -> {
            popOverHideTimer.stop();
            if (!popOver.isShowing()) {
                popOver.show(this);
            }
        });
        addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
            double x = event.getScreenX();
            double y = event.getScreenY();
            Bounds bounds = localToScreen(getBoundsInLocal());

            if (bounds.contains(x, y)) {
                // 鼠标仍在标签内，该事件可能由PopOver弹出引起，要阻止其继续传递
                event.consume();

                return;
            }
            popOverHideTimer.restart();
        });
        popOverContentPane.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> popOverHideTimer.stop());
        popOverContentPane.addEventFilter(MouseEvent.MOUSE_EXITED, event -> popOverHideTimer.restart());
        setGraphic(icon);
    }

    @Override
    public void destroy() {
        popOverHideTimer.stop();
    }
}
