package io.knifer.freebox.handler.impl;

import cn.hutool.core.util.RuntimeUtil;
import io.knifer.freebox.handler.PlayerCheckHandler;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.util.FileUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Linux下检测VLC播放器是否安装
 *
 * @author Knifer
 */
public class LinuxVLCPlayerCheckHandler implements PlayerCheckHandler {

    private final Pattern vlcLibPattern;

    public LinuxVLCPlayerCheckHandler() {
        vlcLibPattern = Pattern.compile("libvlc\\.so(?:\\.\\d)*");
    }

    @Override
    public boolean handle() {
        String execResult = RuntimeUtil.execForStr("which vlc");

        if (StringUtils.isNotBlank(execResult) && !execResult.contains("no vlc")) {

            return true;
        }

        return FileUtil.existsSubFile(ConfigHelper.getVlcPath(), vlcLibPattern);
    }
}
