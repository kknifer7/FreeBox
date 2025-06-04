package io.knifer.freebox.helper;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import io.knifer.freebox.exception.FBException;
import javafx.application.Platform;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;
import java.nio.file.Path;

/**
 * 系统相关
 *
 * @author Knifer
 */
@UtilityClass
public class SystemHelper {

    private final static Timer THREAD_EXECUTION_STATE_TIMER = new Timer(60000, ignored -> sendThreadExecutionState());

    private final static Path LOCAL_STORAGE_PATH;

    static {
        if (!SystemUtils.IS_OS_WINDOWS) {
            throw new FBException("Only Windows is supported");
        }
        LOCAL_STORAGE_PATH = Path.of(
                System.getProperty("user.home"), "AppData", "Local", "FreeBox"
        );
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

    public Path getLocalStoragePath() {
        return LOCAL_STORAGE_PATH;
    }
}
