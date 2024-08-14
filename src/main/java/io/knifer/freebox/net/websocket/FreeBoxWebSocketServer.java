package io.knifer.freebox.net.websocket;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * WebSocket服务
 *
 * @author Knifer
 */
@Slf4j
public class FreeBoxWebSocketServer extends WebSocketServer {

	public FreeBoxWebSocketServer(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		log.info("new connection to {}: {}", conn.getRemoteSocketAddress(), handshake.getResourceDescriptor());
		conn.send("Welcome to the server!"); //This method sends a message to the new client
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		log.info(
				"closed {} with exit code {}, additional info: {}",
				conn.getRemoteSocketAddress(), code, StringUtils.isBlank(reason) ? "-" : reason
		);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		System.out.println("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		System.out.println("received ByteBuffer from "	+ conn.getRemoteSocketAddress());
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		log.error("an error occurred on connection {}", conn.getRemoteSocketAddress(), ex);
	}
	
	@Override
	public void onStart() {
		log.info("WebSocket Service start successfully.");
	}
}