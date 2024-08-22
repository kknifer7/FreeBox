package io.knifer.freebox.net.websocket.core;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.constant.BaseValues;
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

    private final static KebSocketTopicKeeper INSTANCE = new KebSocketTopicKeeper();

    private KebSocketTopicKeeper() {}

    public static KebSocketTopicKeeper getInstance() {
        return INSTANCE;
    }

    public void addTopic(Message<JsonElement> message) {
        DATA_MAP.put(message.getTopicId(), message);
    }

    public <T> Future<T> getTopic(String topicId, TypeToken<T> typeToken) {
        return CompletableFuture.supplyAsync(() -> {
            Message<JsonElement> message = null;

            try {
                message = DATA_MAP.take(topicId, BaseValues.KEB_SOCKET_REQUEST_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            if (message == null) {
                return null;
            }

            return GsonUtil.fromJson(message.getData(), typeToken);
        }, EXECUTOR);
    }

    public void destroy() {
        // TODO 销毁时要调用
        DATA_MAP.clear();
        EXECUTOR.shutdown();
    }
}
