package io.knifer.freebox.net.websocket.core;

import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.model.common.Message;
import io.knifer.freebox.util.GsonUtil;
import lombok.AllArgsConstructor;
import org.java_websocket.WebSocket;

import java.util.concurrent.Future;

/**
 * 通信执行器
 *
 * @author Knifer
 */
@AllArgsConstructor
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

        return topicKeeper.getTopic(topic.getTopicId(), resultData);
    }

    public <T, R> Future<R> sendTopic(WebSocket connection, Integer code, T data, String topicId, TypeToken<R> resultData) {
        Message<T> topic = Message.topic(code, data, topicId);

        send(connection, topic);

        return topicKeeper.getTopic(topic.getTopicId(), resultData);
    }

    public <T> void send(WebSocket connection, Message<T> message) {
        connection.send(GsonUtil.toJson(message));
    }
}
