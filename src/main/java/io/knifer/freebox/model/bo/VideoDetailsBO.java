package io.knifer.freebox.model.bo;

import io.knifer.freebox.component.node.VLCPlayer;
import io.knifer.freebox.model.common.AbsXml;
import lombok.Data;

/**
 * 影片详情界面携带数据
 *
 * @author Knifer
 */
@Data
public class VideoDetailsBO {

    /**
     * 影片详情数据
     */
    private AbsXml videoDetail;

    /**
     * 播放器
     */
    private VLCPlayer player;

    public static VideoDetailsBO of(AbsXml videoDetail, VLCPlayer player) {
        VideoDetailsBO result = new VideoDetailsBO();

        result.setVideoDetail(videoDetail);
        result.setPlayer(player);

        return result;
    }
}
