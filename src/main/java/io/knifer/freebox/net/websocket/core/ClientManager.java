package io.knifer.freebox.net.websocket.core;

import io.knifer.freebox.model.domain.ClientInfo;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端管理器
 *
 * @author Knifer
 */
@Slf4j
public class ClientManager {

    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();

    public void register(ClientInfo clientInfo, WebSocket connection) {
        clients.put(connection.getRemoteSocketAddress().getHostName(), clientInfo);
        log.info("register client: {}", clientInfo);
    }

    public ClientInfo unregister(ClientInfo clientInfo) {
        return unregister(clientInfo.getConnection());
    }

    public ClientInfo unregister(WebSocket connection) {
        if (connection.isOpen()) {
            connection.close();
        }

        return clients.remove(connection.getRemoteSocketAddress().getHostName());
    }

    public Optional<ClientInfo> getClientInfo(WebSocket connection) {
        return Optional.ofNullable(clients.get(connection.getRemoteSocketAddress().getHostName()));
    }

    public boolean isRegistered(WebSocket connection) {
        return clients.containsKey(connection.getRemoteSocketAddress().getHostName());
    }

    public Collection<ClientInfo> getClients() {
        return clients.values();
    }
}
