package io.knifer.freebox.constant;

import io.knifer.freebox.model.domain.ClientInfo;
import lombok.experimental.UtilityClass;

/**
 * FreeBox事件
 * 对于无需传参的事件类型，在post时可以直接使用AppEvents下的静态实例
 * 对于需要传参的事件类型，在post时需要new出来
 *
 * @author Knifer
 */
@UtilityClass
public class AppEvents {

    public static final AppInitializedEvent APP_INITIALIZED = new AppInitializedEvent();

    public interface Event {}

    public static final class AppInitializedEvent implements Event {}

    public record ClientRegisteredEvent(ClientInfo clientInfo) implements Event {}

    public record ClientUnregisteredEvent(ClientInfo clientInfo) implements Event {}
}
