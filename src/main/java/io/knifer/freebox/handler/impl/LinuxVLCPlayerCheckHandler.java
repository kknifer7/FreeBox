package io.knifer.freebox.handler.impl;

import cn.hutool.core.util.RuntimeUtil;
import io.knifer.freebox.handler.VLCPlayerCheckHandler;
import org.apache.commons.lang3.StringUtils;

/**
 * Linux下检测VLC播放器是否安装
 *
 * @author Knifer
 */
public class LinuxVLCPlayerCheckHandler implements VLCPlayerCheckHandler {

    @Override
    public boolean handle() {
        String execResult = RuntimeUtil.execForStr("which vlc");

        return StringUtils.isNotBlank(execResult) && !execResult.contains("no vlc");
    }
}
