package io.knifer.freebox.component.node;

import io.knifer.freebox.model.domain.LiveChannel;
import javafx.scene.control.Label;
import lombok.Getter;

/**
 * 播放器（直播模式） - 频道线路标签
 *
 * @author Knifer
 */
@Getter
public class VLCPlayerLiveChannelLineLabel extends Label {

    private final LiveChannel.Line liveChannelLine;

    public VLCPlayerLiveChannelLineLabel(LiveChannel.Line liveChannelLine) {
        super(liveChannelLine.getTitle());
        getStyleClass().add("vlc-player-live-channel-line-label");
        this.liveChannelLine = liveChannelLine;
    }
}
