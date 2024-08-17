package io.knifer.freebox.net.websocket.handler;

import org.java_websocket.WebSocket;

/**
 * WS消息处理器
 *
 * @author Knifer
 */
public interface KebSocketMessageHandler {

    /**
     * 是否支持指定的消息码
     * @see io.knifer.freebox.constant.MessageCodes
     * @param code 消息码
     * @return bool
     */
    boolean support(Integer code);

    /**
     * 处理消息
     * @param messageData 消息数据
     * @param connection 客户端连接对象
     */
    void handle(String messageData, WebSocket connection);
}
