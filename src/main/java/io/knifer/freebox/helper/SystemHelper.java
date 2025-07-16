package io.knifer.freebox.helper;

import cn.hutool.core.util.RuntimeUtil;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import io.knifer.freebox.exception.FBException;
import javafx.application.Platform;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;

/**
 * 系统相关
 *
 * @author Knifer
 */
@UtilityClass
public class SystemHelper {

    private final static Timer THREAD_EXECUTION_STATE_TIMER =
            new Timer(60000, ignored -> sendThreadExecutionState());

    private final static io.knifer.freebox.constant.Platform CURRENT_PLATFORM;

    static {
        String exeResult;

        // 确定运行平台
        if (SystemUtils.IS_OS_WINDOWS) {
            CURRENT_PLATFORM = io.knifer.freebox.constant.Platform.WINDOWS;
        } else if (SystemUtils.IS_OS_MAC) {
            CURRENT_PLATFORM = io.knifer.freebox.constant.Platform.MAC;
        } else if (SystemUtils.IS_OS_LINUX) {
            if (
                    StringUtils.isNotBlank((exeResult = RuntimeUtil.execForStr("which dpkg"))) &&
                    !StringUtils.contains(exeResult, "no dpkg")
            ) {
                CURRENT_PLATFORM = io.knifer.freebox.constant.Platform.DEB_LINUX;
            } else if (
                    StringUtils.isNotBlank((exeResult = RuntimeUtil.execForStr("which rpm"))) &&
                    !StringUtils.contains(exeResult, "no rpm")
            ) {
                CURRENT_PLATFORM = io.knifer.freebox.constant.Platform.RPM_LINUX;
            } else {
                CURRENT_PLATFORM = io.knifer.freebox.constant.Platform.OTHER_LINUX;
            }
        } else {
            throw new FBException("unsupported platform");
        }
    }

    private void sendThreadExecutionState() {
        Platform.runLater(() -> {
            if (SystemUtils.IS_OS_WINDOWS) {
                Kernel32.INSTANCE.SetThreadExecutionState(
                        WinBase.ES_CONTINUOUS |
                                WinBase.ES_SYSTEM_REQUIRED |
                                WinBase.ES_DISPLAY_REQUIRED
                );
            }
        });
    }

    public void allowSleep() {
        clearThreadExecutionState();
        THREAD_EXECUTION_STATE_TIMER.stop();
    }

    private void clearThreadExecutionState() {
        Platform.runLater(() -> {
            if (SystemUtils.IS_OS_WINDOWS) {
                Kernel32.INSTANCE.SetThreadExecutionState(WinBase.ES_CONTINUOUS);
            }
        });
    }

    public void preventSleep() {
        THREAD_EXECUTION_STATE_TIMER.start();
    }

    public io.knifer.freebox.constant.Platform getPlatform() {
        return CURRENT_PLATFORM;
    }
}
