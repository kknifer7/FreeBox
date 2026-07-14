package io.knifer.freebox.net.websocket.core;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.MessageCodes;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.model.common.tvbox.Message;
import io.knifer.freebox.component.juc.InterruptibleCompletableFuture;
import io.knifer.freebox.util.json.GsonUtil;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.thavam.util.concurrent.blockingMap.BlockingHashMap;
import org.thavam.util.concurrent.blockingMap.BlockingMap;

import java.util.concurrent.*;

/**
 * Topic保管
 * 接收来的Topic消息都会在这里保管和取用
 *
 * @author Knifer
 */
@Slf4j
@Singleton
public class KebSocketTopicKeeper {

    private final BlockingMap<String, Message<JsonElement>> DATA_MAP = new BlockingHashMap<>();

    private final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r);

                t.setName("KebSocket-Thread");
                t.setUncaughtExceptionHandler(GlobalExceptionHandler.getInstance());

                return t;
            }
    );

    private final ThreadPoolExecutor SEARCH_EXECUTOR = new ThreadPoolExecutor(
            3,
            3,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r);

                t.setName("KebSocket-Search-Thread");
                t.setUncaughtExceptionHandler(GlobalExceptionHandler.getInstance());

                return t;
            }
    );

    public void addTopic(Message<JsonElement> message) {
        DATA_MAP.put(message.getTopicId(), message);
    }

    public <T> CompletableFuture<T> getTopic(String topicId, TypeToken<T> typeToken, Integer code) {
        ThreadPoolExecutor executor = code == MessageCodes.GET_SEARCH_CONTENT ? SEARCH_EXECUTOR : EXECUTOR;
        CompletableFuture<T> future = new CompletableFuture<>();
        Future<?> task;

        task = executor.submit(() -> {
            Message<JsonElement> message;
            JsonElement jsonData;

            try {
                message = DATA_MAP.take(topicId, BaseValues.KEB_SOCKET_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                if (message == null) {
                    future.complete(null);

                    return;
                }
                jsonData = message.getData();
                if (jsonData == null) {
                    future.complete(null);

                    return;
                }
                future.complete(GsonUtil.fromJson(jsonData, typeToken));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(new CancellationException("Topic cancelled: " + topicId));
            } catch (Exception e) {
                log.error("getTopic error", e);
                future.complete(null);
            }
        });

        return new InterruptibleCompletableFuture<>(future, task);
    }

    public void destroy() {
        log.info("destroy KebSocketTopicKeeper......");
        DATA_MAP.clear();
        EXECUTOR.shutdownNow();
        SEARCH_EXECUTOR.shutdownNow();
    }
}
