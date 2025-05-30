package io.knifer.freebox.net.websocket.core;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.model.common.tvbox.Message;
import io.knifer.freebox.net.websocket.exception.ForbiddenException;
import io.knifer.freebox.net.websocket.handler.KebSocketMessageHandler;
import io.knifer.freebox.net.websocket.handler.impl.ClientRegisterHandler;
import io.knifer.freebox.net.websocket.handler.impl.CommonTopicHandler;
import io.knifer.freebox.net.websocket.handler.impl.ValidationHandler;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.json.GsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;

import java.util.List;

/**
 * 消息分发器
 *
 * @author Knifer
 */
@Slf4j
public class KebSocketMessageDispatcher {

    private final List<KebSocketMessageHandler<?>> handlers;

    public KebSocketMessageDispatcher(ClientManager clientManager) {
        handlers = ImmutableList.of(
                new ValidationHandler(clientManager),
                new ClientRegisterHandler(clientManager),
                new CommonTopicHandler(KebSocketTopicKeeper.getInstance())
        );
    }

    public void dispatch(String message, WebSocket connection) {
        Message<Object> msgUnResolved;
        Message<?> msg;
        Integer code;

        try {
            msgUnResolved = GsonUtil.fromJson(message, new TypeToken<>(){});
        } catch (JsonSyntaxException e) {
            connection.close();
            log.warn("ip [{}] send wrong message, closed", connection.getRemoteSocketAddress().getHostName());

            return;
        }
        code = msgUnResolved.getCode();
        if (code == null) {
            connection.close();
            log.warn("ip [{}] send wrong message, closed", connection.getRemoteSocketAddress().getHostName());

            return;
        }
        try {
            for (KebSocketMessageHandler<?> handler : handlers) {
                if (handler.support(msgUnResolved)) {
                    msg = handler.resolve(message);
                    handler.handle(CastUtil.cast(msg), connection);
                }
            }
        } catch (ForbiddenException e) {
            connection.close();
            log.warn("ip [{}] send wrong message, closed", e.getIp());
        }
    }
}
