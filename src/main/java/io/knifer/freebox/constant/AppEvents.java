package io.knifer.freebox.constant;

import io.knifer.freebox.model.domain.ClientInfo;
import lombok.experimental.UtilityClass;

/**
 * FreeBox事件
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
