package io.knifer.freebox.helper;

import cn.hutool.core.util.RuntimeUtil;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import io.knifer.freebox.constant.Architecture;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.Platform;
import io.knifer.freebox.exception.FBException;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ArchUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.arch.Processor;

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
    private final static Architecture CURRENT_ARCHITECTURE;
    private final static boolean DEBUG_FLAG = "true".equals(BaseResources.X_PROPERTIES.getProperty(BaseValues.X_DEBUG));

    static {
        String exeResult;
        Processor processor;

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
        // 确定运行架构
        processor = ArchUtils.getProcessor();
        if (!processor.is64Bit()) {
            throw new FBException("unsupported architecture");
        }
        switch (processor.getType()) {
            case X86 -> CURRENT_ARCHITECTURE = Architecture.AMD64;
            case AARCH_64 -> CURRENT_ARCHITECTURE = Architecture.ARM64;
            default -> throw new FBException("unsupported architecture");
        }
    }

    private void sendThreadExecutionState() {
        switch (CURRENT_PLATFORM) {
            case WINDOWS -> Kernel32.INSTANCE.SetThreadExecutionState(
                    WinBase.ES_CONTINUOUS |
                            WinBase.ES_SYSTEM_REQUIRED |
                            WinBase.ES_DISPLAY_REQUIRED
            );
            case MAC -> RuntimeUtil.exec("caffeinate -di -t 60");
            case DEB_LINUX, RPM_LINUX, OTHER_LINUX -> RuntimeUtil.exec("xdg-screensaver reset");
        }
    }

    public void allowSleep() {
        clearThreadExecutionState();
        THREAD_EXECUTION_STATE_TIMER.stop();
    }

    private void clearThreadExecutionState() {
        if (CURRENT_PLATFORM == Platform.WINDOWS) {
            Kernel32.INSTANCE.SetThreadExecutionState(WinBase.ES_CONTINUOUS);
        }
    }

    public void preventSleep() {
        THREAD_EXECUTION_STATE_TIMER.start();
    }

    public io.knifer.freebox.constant.Platform getPlatform() {
        return CURRENT_PLATFORM;
    }

    public Architecture getArchitecture() {
        return CURRENT_ARCHITECTURE;
    }

    public boolean isDebug() {
        return DEBUG_FLAG;
    }
}
