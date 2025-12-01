package io.knifer.freebox.component.node.player;

import io.knifer.freebox.model.domain.LiveChannel;
import javafx.scene.control.Label;
import lombok.Getter;

/**
 * 播放器（直播模式） - 频道线路标签
 *
 * @author Knifer
 */
@Getter
public class PlayerLiveChannelLineLabel extends Label {

    private final LiveChannel.Line liveChannelLine;

    public PlayerLiveChannelLineLabel(LiveChannel.Line liveChannelLine) {
        super(liveChannelLine.getTitle());
        getStyleClass().add("player-live-channel-line-label");
        this.liveChannelLine = liveChannelLine;
    }
}
