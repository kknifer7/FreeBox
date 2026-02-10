package io.knifer.freebox.net.websocket.core;

import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.model.common.tvbox.Message;
import io.knifer.freebox.util.json.GsonUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;

import java.util.concurrent.Future;

/**
 * 通信执行器
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Singleton
public class KebSocketRunner {

    private final KebSocketTopicKeeper topicKeeper;

    public <T> void send(WebSocket connection, Integer code, T data) {
        send(connection, Message.oneWay(code, data));
    }

    public <T> void send(WebSocket connection, Integer code, T data, String topicId) {
        send(connection, Message.oneWay(code, data, topicId));
    }

    public <T, R> Future<R> sendTopic(WebSocket connection, Integer code, T data, TypeToken<R> resultData) {
        Message<T> topic = Message.topic(code, data);

        send(connection, topic);

        return topicKeeper.getTopic(topic.getTopicId(), resultData, code);
    }

    public <T, R> Future<R> sendTopic(WebSocket connection, Integer code, T data, String topicId, TypeToken<R> resultData) {
        Message<T> topic = Message.topic(code, data, topicId);

        send(connection, topic);

        return topicKeeper.getTopic(topic.getTopicId(), resultData, code);
    }

    public <T> void send(WebSocket connection, Message<T> message) {
        log.info("send message: {}", message);
        connection.send(GsonUtil.toJson(message));
    }
}
