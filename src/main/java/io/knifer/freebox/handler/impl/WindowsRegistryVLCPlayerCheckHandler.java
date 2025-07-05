package io.knifer.freebox.handler.impl;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.handler.VLCPlayerCheckHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * 注册表方式检测 VCL Player 是否安装
 *
 * @author Knifer
 */
@Slf4j
public class WindowsRegistryVLCPlayerCheckHandler implements VLCPlayerCheckHandler {

    @Override
    public boolean handle() {
        if (!SystemUtils.IS_OS_WINDOWS) {
            throw new FBException("only support windows.");
        }
        try {
            String[] keys = Advapi32Util.registryGetKeys(WinReg.HKEY_CLASSES_ROOT, "Applications");

            return ArrayUtils.contains(keys, "vlc.exe");
        } catch (Throwable e) {
            log.error("search vlc player failed", e);
            throw new FBException("search vlc player failed", e);
        }
    }
}
