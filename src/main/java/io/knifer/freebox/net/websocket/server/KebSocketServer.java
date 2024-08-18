package io.knifer.freebox.net.websocket.server;

import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.core.KebSocketMessageDispatcher;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * KebSocket服务
 * 基于WebSocket封装
 *
 * @author Knifer
 */
@Slf4j
public class KebSocketServer extends WebSocketServer {

	private final ClientManager clientManager = new ClientManager();

	private final KebSocketMessageDispatcher messageDispatcher = new KebSocketMessageDispatcher(clientManager);

	public KebSocketServer(InetSocketAddress address) {
		super(address);
	}

	public ClientManager getClientManager() {
		return clientManager;
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		log.info("new connection to {}: {}", conn.getRemoteSocketAddress(), handshake.getResourceDescriptor());
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		ClientInfo clientInfo = clientManager.unregister(conn);

		log.info(
				"closed {} with exit code {}, additional info: {}",
				conn.getRemoteSocketAddress(), code, StringUtils.isBlank(reason) ? "-" : reason
		);
		if (clientInfo == null) {
			return;
		}
		Platform.runLater(() -> {
			Context.INSTANCE.postEvent(new AppEvents.ClientUnregisteredEvent(clientInfo));
			ToastHelper.showInfoI18n(I18nKeys.MESSAGE_CLIENT_UNREGISTERED, conn.getRemoteSocketAddress().getHostName());
		});
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		log.info("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
		messageDispatcher.dispatch(message, conn);
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		log.info("received ByteBuffer from "	+ conn.getRemoteSocketAddress());
		conn.close();
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