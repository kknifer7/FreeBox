package io.knifer.freebox.net.websocket.handler;

import io.knifer.freebox.model.common.tvbox.Message;
import org.java_websocket.WebSocket;

/**
 * WS消息处理器
 *
 * @author Knifer
 */
public interface KebSocketMessageHandler<T> {

    /**
     * 是否支持指定的消息
     * @see io.knifer.freebox.constant.MessageCodes
     * @param message 消息
     * @return bool
     */
    boolean support(Message<?> message);

    /**
     * 解析Message对象
     * @param messageString 消息字符串
     * @return Message对象
     */
    Message<T> resolve(String messageString);

    /**
     * 处理消息
     * @param message 消息
     * @param connection 客户端连接对象
     */
    void handle(Message<T> message, WebSocket connection) throws Exception;
}
