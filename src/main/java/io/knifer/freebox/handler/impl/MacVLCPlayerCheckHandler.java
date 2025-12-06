package io.knifer.freebox.handler.impl;

import cn.hutool.core.util.RuntimeUtil;
import io.knifer.freebox.handler.PlayerCheckHandler;
import org.apache.commons.lang3.StringUtils;

/**
 * macos下检测VLC播放器是否安装
 *
 * @author Knifer
 */
public class MacVLCPlayerCheckHandler implements PlayerCheckHandler {

    @Override
    public boolean handle() {
        String execResult = RuntimeUtil.execForStr("ls /Applications | grep VLC.app");

        return StringUtils.isNotBlank(execResult);
    }
}
