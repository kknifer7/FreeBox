package io.knifer.freebox.handler.impl;

import io.knifer.freebox.constant.PlayerType;
import io.knifer.freebox.handler.PlayerCheckHandler;
import io.knifer.freebox.helper.ConfigHelper;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 检测外部播放器是否存在
 *
 * @author Knifer
 */
@Slf4j
public class ExternalPlayerCheckHandler implements PlayerCheckHandler {
    @Override
    public boolean handle() {
        PlayerType playerType = ConfigHelper.getPlayerType();
        String pathStr;
        Path path;

        if (playerType == PlayerType.MPV_EXTERNAL) {
            pathStr = ConfigHelper.getMpvPath();
        } else {
            throw new IllegalArgumentException("invalid player type");
        }
        try {
            path = Paths.get(pathStr);
        } catch (InvalidPathException e) {
            log.warn("invalid external player path: {}", pathStr);

            return false;
        }
        if (Files.isExecutable(path)) {
            log.debug("external player found: {}", path);

            return true;
        }

        return false;
    }
}
