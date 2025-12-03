package io.knifer.freebox.model.domain;

import io.github.filelize.Filelize;
import io.github.filelize.FilelizeType;
import io.github.filelize.Id;
import lombok.Data;

/**
 * 客户端Live界面属性
 *
 * @author Knifer
 */
@Data
@Filelize(name = "client_live_properties", type = FilelizeType.MULTIPLE_FILES, directory = "client_live_properties")
public class ClientLiveProperties implements Savable, Copyable<ClientLiveProperties> {

    /**
     * id
     */
    @Id
    private String id;
    /**
     * 最后使用的直播源标题
     * 如果是独立直播源，该项为空
     * @see io.knifer.freebox.constant.ClientType
     */
    private String liveSourceNameLastUsed;
    /**
     * 最后使用的频道组标题
     */
    private String liveChannelGroupTitleLastUsed;
    /**
     * 最后使用的频道标题
     */
    private String liveChannelTitleLastUsed;
    /**
     * 最后使用的频道线路标题
     */
    private String liveChannelLineTitleLastUsed;

    public static ClientLiveProperties from(String id) {
        ClientLiveProperties result = new ClientLiveProperties();

        result.id = id;

        return result;
    }

    @Override
    public ClientLiveProperties copy() {
        ClientLiveProperties result = new ClientLiveProperties();

        result.liveChannelGroupTitleLastUsed = liveChannelGroupTitleLastUsed;
        result.liveChannelTitleLastUsed = liveChannelTitleLastUsed;
        result.liveChannelLineTitleLastUsed = liveChannelLineTitleLastUsed;

        return result;
    }
}
