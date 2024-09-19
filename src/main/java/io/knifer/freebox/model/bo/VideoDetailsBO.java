package io.knifer.freebox.model.bo;

import io.knifer.freebox.component.node.VLCPlayer;
import io.knifer.freebox.model.common.AbsXml;
import io.knifer.freebox.model.common.SourceBean;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.net.websocket.template.KebSocketTemplate;
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
     * 播放源
     */
    private SourceBean source;

    /**
     * 播放器
     */
    private VLCPlayer player;

    /**
     * KebSocket通信模板对象
     */
    private KebSocketTemplate template;

    /**
     * 客户端信息
     */
    private ClientInfo clientInfo;

    public static VideoDetailsBO of(
            AbsXml videoDetail, SourceBean source,
            VLCPlayer player,
            KebSocketTemplate template,
            ClientInfo clientInfo
    ) {
        VideoDetailsBO result = new VideoDetailsBO();

        result.setVideoDetail(videoDetail);
        result.setSource(source);
        result.setPlayer(player);
        result.setTemplate(template);
        result.setClientInfo(clientInfo);

        return result;
    }
}
