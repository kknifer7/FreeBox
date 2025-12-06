package io.knifer.freebox.model.bo;

import io.knifer.freebox.component.node.player.BasePlayer;
import io.knifer.freebox.model.common.tvbox.AbsXml;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.spider.template.SpiderTemplate;
import lombok.Data;

import java.util.function.Consumer;

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
     * 影片播放信息
     */
    private VideoPlayInfoBO playInfo;

    /**
     * 播放源
     */
    private SourceBean source;

    /**
     * 播放器
     */
    private BasePlayer<?> player;

    /**
     * KebSocket通信模板对象
     */
    private SpiderTemplate template;

    /**
     * 界面关闭时回调
     */
    private Consumer<VideoPlayInfoBO> onClose;

    public static VideoDetailsBO of(
            AbsXml videoDetail,
            VideoPlayInfoBO playInfo,
            SourceBean source,
            BasePlayer<?> player,
            SpiderTemplate template,
            Consumer<VideoPlayInfoBO> onClose
    ) {
        VideoDetailsBO result = new VideoDetailsBO();

        result.setVideoDetail(videoDetail);
        result.setPlayInfo(playInfo);
        result.setSource(source);
        result.setPlayer(player);
        result.setTemplate(template);
        result.setOnClose(onClose);

        return result;
    }
}
