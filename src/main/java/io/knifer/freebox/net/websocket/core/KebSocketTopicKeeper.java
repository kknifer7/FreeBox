package io.knifer.freebox.net.websocket.core;

import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.model.common.Message;
import io.knifer.freebox.util.GsonUtil;
import org.thavam.util.concurrent.blockingMap.BlockingHashMap;
import org.thavam.util.concurrent.blockingMap.BlockingMap;

import java.util.concurrent.*;

/**
 * Topic保管
 * 接收来的Topic消息都会在这里保管和取用
 *
 * @author Knifer
 */
public class KebSocketTopicKeeper {

    private final BlockingMap<String, Message<String>> DATA_MAP = new BlockingHashMap<>();

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

    private final long TIMEOUT_SECONDS = 5;

    public void addTopic(Message<String> message) {
        DATA_MAP.put(message.getTopicId(), message);
    }

    public <T> Future<T> getTopic(String topicId, Class<T> resultDataClazz) {
        return CompletableFuture.supplyAsync(() -> {
            Message<String> message = null;

            try {
                message = DATA_MAP.take(topicId, TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            if (message == null) {
                return null;
            }

            return GsonUtil.fromJson(message.getData(), resultDataClazz);
        }, EXECUTOR);
    }

    public void destroy() {
        DATA_MAP.clear();
        EXECUTOR.shutdown();
    }
}
