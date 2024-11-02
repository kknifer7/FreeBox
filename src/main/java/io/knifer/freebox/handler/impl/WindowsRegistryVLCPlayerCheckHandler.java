package io.knifer.freebox.handler.impl;

import com.jianggujin.registry.JExecResult;
import com.jianggujin.registry.JQueryOptions;
import com.jianggujin.registry.JRegistry;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.handler.VLCPlayerCheckHandler;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;

/**
 * 注册表方式检测 VCL Player 是否安装
 *
 * @author Knifer
 */
public class WindowsRegistryVLCPlayerCheckHandler implements VLCPlayerCheckHandler {

    private final JQueryOptions QUERY_OPTIONS = new JQueryOptions().useF("\"vlc.exe\"");

    @Override
    public boolean handle() {
        JExecResult result;

        if (!SystemUtils.IS_OS_WINDOWS) {
            throw new FBException("only support windows.");
        }
        try {
            result = JRegistry.query("HKEY_CLASSES_ROOT\\Applications\\", QUERY_OPTIONS);
        } catch (IOException | InterruptedException e) {
            throw new FBException("search vlc player failed", e);
        }

        return result.isSuccess();
    }
}
