package io.knifer.freebox.constant;

import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.net.websocket.server.KebSocketServer;
import lombok.experimental.UtilityClass;

/**
 * FreeBox事件
 * 对于无需传参的事件类型，在post时可以直接使用AppEvents下的静态实例或类模板
 * 对于需要传参的事件类型，在post时需要new出来
 *
 * @author Knifer
 */
@UtilityClass
public class AppEvents {

    public static final AppInitializedEvent APP_INITIALIZED = new AppInitializedEvent();

    public static final HttpServerStartedEvent HTTP_SERVER_STARTED = new HttpServerStartedEvent();

    public static final SettingsSavedEvent SETTINGS_SAVED = new SettingsSavedEvent();

    public interface Event {}

    public static final class AppInitializedEvent implements Event {}

    public record WsServerStartedEvent(KebSocketServer server) implements Event {}

    public static final class HttpServerStartedEvent implements Event {}

    public static final class SettingsSavedEvent implements Event {}

    public record ClientRegisteredEvent(ClientInfo clientInfo) implements Event {}

    public record ClientUnregisteredEvent(ClientInfo clientInfo) implements Event {}

    public record UsageFontChangedEvent(String fontFamily) implements Event {}
}
