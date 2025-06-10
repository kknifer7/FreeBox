package io.knifer.freebox.net.websocket.core;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.helper.LoadingHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.domain.ClientInfo;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 客户端管理器
 *
 * @author Knifer
 */
@Slf4j
public class ClientManager {

    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();

    private final AtomicReference<ClientInfo> currentClient = new AtomicReference<>();

    private final CyclicBarrier currentClientBarrier = new CyclicBarrier(2);

    private ThreadPoolExecutor connectingExecutor = null;

    public void register(ClientInfo clientInfo) {
        clients.put(clientInfo.getId(), clientInfo);
        log.info("register client: {}", clientInfo);
    }

    public ClientInfo unregister(ClientInfo clientInfo) {
        ClientInfo result = clients.remove(clientInfo.getId());
        WebSocket connection;

        if (result == null) {
            return null;
        }
        connection = result.getConnection();
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        log.info("unregister client: {}", result);

        return result;
    }

    public ClientInfo unregister(WebSocket connection) {
        Optional<Map.Entry<String, ClientInfo>> entryOptional;

        if (connection.isOpen()) {
            connection.close();
        }
        entryOptional = clients.entrySet()
                .stream()
                .filter(entry -> {
                    WebSocket conn = entry.getValue().getConnection();

                    return conn != null && conn.equals(connection);
                })
                .findFirst();

        return entryOptional.map(
                stringClientInfoEntry ->
                        clients.remove(stringClientInfoEntry.getValue().getId())
        ).orElse(null);

    }

    public boolean isRegistered(ClientInfo clientInfo) {
        return clients.containsKey(clientInfo.getId());
    }

    public boolean isRegistered(WebSocket connection) {
        String hostName = connection.getRemoteSocketAddress().getHostName();

        return clients.values()
                .stream()
                .anyMatch(clientInfo -> {
                    WebSocket conn = clientInfo.getConnection();

                    return conn != null && conn.getRemoteSocketAddress().getHostName().equals(hostName);
                });
    }

    public CompletableFuture<ClientInfo> getCurrentClient() {
        return CompletableFuture.supplyAsync(
                () -> {
                    ClientInfo clientInfo = getCurrentClientImmediately();

                    if (clientInfo != null && clientInfo.isOpen()) {
                        return clientInfo;
                    }
                    Platform.runLater(() -> LoadingHelper.showWaitingReconnecting(Context.INSTANCE.getCurrentStage()));
                    log.info("client disconnected, waiting for reconnecting");
                    try {
                        // 等待重连
                        currentClientBarrier.await();
                        clientInfo = currentClient.get();
                        currentClientBarrier.reset();
                        Platform.runLater(LoadingHelper::hideWaitingReconnecting);
                        log.info("client reconnected");

                        return clientInfo;
                    } catch (InterruptedException e) {
                        currentClientBarrier.reset();
                        log.info("user give up reconnecting");

                        return null;
                    } catch (BrokenBarrierException e) {
                        Platform.runLater(() -> ToastHelper.showException(e));

                        return null;
                    }
                },
                connectingExecutor
        );
    }

    @Nullable
    public ClientInfo getCurrentClientImmediately() {
        return currentClient.get();
    }

    public void clearCurrentClient() {
        currentClient.set(null);
        shutdownConnectingExecutor();
    }

    public void updateCurrentClient(ClientInfo clientInfo) {
        currentClient.set(clientInfo);
        connectingExecutor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r);

                    t.setName("Client-Connecting-Thread|" + clientInfo.getName());
                    t.setUncaughtExceptionHandler(GlobalExceptionHandler.getInstance());

                    return t;
                }
        );
        if (currentClientBarrier.getNumberWaiting() == 1) {
            try {
                currentClientBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                ToastHelper.showException(e);
            }
        }
    }

    public void shutdownConnectingExecutor() {
        if (connectingExecutor != null && !connectingExecutor.isShutdown()) {
            connectingExecutor.shutdownNow();
        }
    }
}
