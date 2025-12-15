package io.knifer.freebox.net.websocket.exception;

import lombok.Getter;
import org.java_websocket.WebSocket;

/**
 * 未注册异常
 * 客户端未注册时会抛出
 *
 * @author Knifer
 */
@Getter
public class ForbiddenException extends Exception {

    private final String ip;

    public ForbiddenException(WebSocket webSocket) {
        this.ip = webSocket.getRemoteSocketAddress().getHostString();
    }
}
