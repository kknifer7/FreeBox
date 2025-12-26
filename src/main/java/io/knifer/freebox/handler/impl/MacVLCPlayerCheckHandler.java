package io.knifer.freebox.handler.impl;

import cn.hutool.core.util.RuntimeUtil;
import io.knifer.freebox.handler.PlayerCheckHandler;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.util.FileUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * macos下检测VLC播放器是否安装
 *
 * @author Knifer
 */
public class MacVLCPlayerCheckHandler implements PlayerCheckHandler {

    private final Pattern vlcLibPattern;

    public MacVLCPlayerCheckHandler() {
        vlcLibPattern = Pattern.compile("libvlc\\.dylib");
    }

    @Override
    public boolean handle() {
        String execResult = RuntimeUtil.execForStr("ls /Applications | grep VLC.app");

        if (StringUtils.isNotBlank(execResult)) {

            return true;
        }

        return FileUtil.existsSubFile(ConfigHelper.getVlcPath(), vlcLibPattern);
    }
}
