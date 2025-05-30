package io.knifer.freebox.net.websocket.handler.impl;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.model.common.tvbox.Message;
import io.knifer.freebox.net.websocket.core.KebSocketTopicKeeper;
import io.knifer.freebox.net.websocket.handler.KebSocketMessageHandler;
import io.knifer.freebox.util.json.GsonUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.java_websocket.WebSocket;

/**
 * topic处理器
 *
 * @author Knifer
 */
@AllArgsConstructor
public class CommonTopicHandler implements KebSocketMessageHandler<JsonElement> {

    private final KebSocketTopicKeeper topicKeeper;

    @Override
    public boolean support(Message<?> message) {
        return BooleanUtils.toBoolean(message.getTopicFlag()) || message.getTopicId() != null;
    }

    @Override
    public Message<JsonElement> resolve(String messageString) {
        return GsonUtil.fromJson(messageString, new TypeToken<>(){});
    }

    @Override
    public void handle(Message<JsonElement> message, WebSocket connection) {
        topicKeeper.addTopic(message);
    }
}
