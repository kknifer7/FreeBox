package io.knifer.freebox.model.domain;

import cn.hutool.crypto.digest.DigestUtil;
import com.google.common.base.Strings;
import io.github.filelize.Filelize;
import io.github.filelize.FilelizeType;
import io.github.filelize.Id;
import io.knifer.freebox.constant.ClientType;
import io.knifer.freebox.model.c2s.RegisterInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.java_websocket.WebSocket;

/**
 * 客户端信息
 *
 * @author Knifer
 */
@Getter
@EqualsAndHashCode
@ToString
@Filelize(name = "client_info", type = FilelizeType.MULTIPLE_FILES, directory = "spider_config")
public class ClientInfo implements Savable {

    @Id
    private String id;

    private String clientName;

    private ClientType clientType;

    private WebSocket connection;

    private String configUrl;

    /**
     * 获取名称
     * 目前的策略是：如果connection不为空，则返回connection的远程地址，否则返回clientName
     * @return 名称
     */
    public String getName() {
        if (connection != null) {
            return connection.getRemoteSocketAddress().getHostName();
        }

        return clientName;
    }

    public boolean isOpen() {
        if (clientType == ClientType.CATVOD_SPIDER || clientType == ClientType.SINGLE_LIVE) {

            return true;
        }

        return connection != null && connection.isOpen();
    }

    public static ClientInfo of(RegisterInfo registerInfo, WebSocket connection) {
        ClientInfo clientInfo = new ClientInfo();

        clientInfo.id = Strings.nullToEmpty(registerInfo.getClientId());
        clientInfo.clientName = Strings.nullToEmpty(registerInfo.getClientName());
        clientInfo.clientType = ClientType.TVBOX_K;
        clientInfo.connection = connection;

        return clientInfo;
    }

    public static ClientInfo of(String configUrl, ClientType clientType) {
        ClientInfo clientInfo = new ClientInfo();

        clientInfo.id = DigestUtil.md5Hex(configUrl);
        clientInfo.clientName = configUrl;
        clientInfo.clientType = clientType;
        clientInfo.configUrl = configUrl;

        return clientInfo;
    }
}
