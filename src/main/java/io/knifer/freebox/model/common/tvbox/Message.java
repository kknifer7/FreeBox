package io.knifer.freebox.model.common.tvbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * WebSocket通信报文
 *
 * @author Knifer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message<T> {

    /**
     * 通信码
     * @see io.knifer.freebox.constant.MessageCodes
     */
    private Integer code;

    /**
     * 携带数据
     */
    private T data;

    /**
     * 是否为topic
     * 当发送方希望接收方提供响应时，该字段为true
     */
    private Boolean topicFlag;

    /**
     * topic id
     * 当发送方希望接收方提供响应时，会在消息中附带topicId，接收方发送响应时，会携带该topicId
     */
    private String topicId;

    /**
     * 创建单向消息
     * 单向消息不需要接收方响应
     * @param code 消息码
     * @param data 携带数据
     * @return message
     */
    public static <T> Message<T> oneWay(Integer code, T data) {
        return new Message<>(code, data, false, null);
    }

    /**
     * 创建用于回复topic的单向消息
     * 单向消息不需要接收方响应
     * @param code 消息码
     * @param data 携带数据
     * @param topicId topic id
     * @return message
     */
    public static <T> Message<T> oneWay(Integer code, T data, String topicId) {
        return new Message<>(code, data, false, topicId);
    }

    /**
     * 创建topic消息
     * topic消息需要接收方响应
     * @param code 消息码
     * @param data 携带数据
     * @return message
     */
    public static <T> Message<T> topic(Integer code, T data) {
        return new Message<>(code, data, true, UUID.randomUUID().toString());
    }

    /**
     * 创建topic消息
     * topic消息需要接收方响应
     * @param code 消息码
     * @param data 携带数据
     * @param topicId topic id
     * @return message
     */
    public static <T> Message<T> topic(Integer code, T data, String topicId) {
        return new Message<>(code, data, true, topicId);
    }

}
