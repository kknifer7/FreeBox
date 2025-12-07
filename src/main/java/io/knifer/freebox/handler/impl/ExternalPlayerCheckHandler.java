package io.knifer.freebox.handler.impl;

import cn.hutool.core.util.RuntimeUtil;
import io.knifer.freebox.constant.PlayerType;
import io.knifer.freebox.handler.PlayerCheckHandler;
import io.knifer.freebox.helper.ConfigHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

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
        String[] commands;
        Function<String, Boolean> resultChecker;
        String execResult;
        boolean success;

        if (playerType == PlayerType.MPV_EXTERNAL) {
            pathStr = ConfigHelper.getMpvPath();
            commands = new String[]{ pathStr, "--version" };
            resultChecker = result ->
                    StringUtils.isNotBlank(result) &&
                            result.contains("mpv") &&
                            result.contains("Copyright");
        } else {
            throw new IllegalArgumentException("invalid player type");
        }
        try {
            execResult = RuntimeUtil.execForStr(commands);
            success = resultChecker.apply(execResult);
        } catch (Exception e) {
            log.error("command run failed", e);
            execResult = null;
            success = false;
        }
        log.debug("\ncommand:\n{}\nresult:\n{}\nsuccess:{}", commands, execResult, success);

        return success;
    }
}
