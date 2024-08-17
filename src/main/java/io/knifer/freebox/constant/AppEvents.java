package io.knifer.freebox.constant;

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
}
