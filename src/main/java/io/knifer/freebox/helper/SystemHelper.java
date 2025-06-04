package io.knifer.freebox.helper;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import javafx.application.Platform;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;

/**
 * 系统相关
 *
 * @author Knifer
 */
@UtilityClass
public class SystemHelper {

    private final static Timer THREAD_EXECUTION_STATE_TIMER = new Timer(60000, ignored -> sendThreadExecutionState());

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
}
