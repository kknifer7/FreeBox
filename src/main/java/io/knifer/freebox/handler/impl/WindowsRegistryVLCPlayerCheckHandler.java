package io.knifer.freebox.handler.impl;

import io.knifer.freebox.handler.PlayerCheckHandler;
import io.knifer.freebox.helper.ConfigHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 注册表方式检测 VCL Player 是否安装
 *
 * @author Knifer
 */
@Slf4j
public class WindowsRegistryVLCPlayerCheckHandler implements PlayerCheckHandler {

    @Override
    public boolean handle() {
        String vlcPath;

        try {
            if (new NativeDiscovery().discover()) {

                return true;
            }
            vlcPath = ConfigHelper.getVlcPath();
            if (StringUtils.isBlank(vlcPath)) {

                return false;
            }

            return Files.exists(Path.of(vlcPath, "libvlc.dll"));
        } catch (Exception e) {
            log.warn("search vlc player failed", e);

            return false;
        }
    }
}
