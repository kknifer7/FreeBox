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

    public boolean isRegistered(WebSocket connection) {
        return clients.containsKey(connection.getRemoteSocketAddress().getHostName());
    }

    public CompletableFuture<ClientInfo> getCurrentClient() {
        return CompletableFuture.supplyAsync(
                () -> {
                    ClientInfo clientInfo = getCurrentClientImmediately();

                    if (clientInfo != null && clientInfo.getConnection().isOpen()) {
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

                    t.setName("KebSocket-Connecting-Thread|" + clientInfo.getConnection().getRemoteSocketAddress().getHostName());
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
