package io.knifer.freebox.handler;

import io.knifer.freebox.constant.PlayerType;
import io.knifer.freebox.handler.impl.ExternalPlayerCheckHandler;
import io.knifer.freebox.handler.impl.LinuxVLCPlayerCheckHandler;
import io.knifer.freebox.handler.impl.MacVLCPlayerCheckHandler;
import io.knifer.freebox.handler.impl.WindowsRegistryVLCPlayerCheckHandler;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.SystemHelper;

import java.util.Map;

/**
 * VCLPlayer处理器（检测安装情况）
 *
 * @author Knifer
 */
public interface PlayerCheckHandler {

    boolean handle();

    Map<PlayerType, PlayerCheckHandler> HANDLERS = Map.of(
            PlayerType.MPV_EXTERNAL, new ExternalPlayerCheckHandler(),
            PlayerType.VLC, switch (SystemHelper.getPlatform()) {
                case WINDOWS -> new WindowsRegistryVLCPlayerCheckHandler();
                case DEB_LINUX, RPM_LINUX, OTHER_LINUX -> new LinuxVLCPlayerCheckHandler();
                case MAC -> new MacVLCPlayerCheckHandler();
            }
    );

    static PlayerCheckHandler select() {
        return HANDLERS.get(ConfigHelper.getPlayerType());
    }
}
