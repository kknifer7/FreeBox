package io.knifer.freebox.exception;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.ToastHelper;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理
 *
 * @author Knifer
 */
@Slf4j
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final GlobalExceptionHandler INSTANCE = new GlobalExceptionHandler();

    public static GlobalExceptionHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (Context.INSTANCE.isInitialized()) {
            if (Platform.isFxApplicationThread()) {
                ToastHelper.showException(e);
            } else {
                Platform.runLater(() -> ToastHelper.showException(e));
            }
        }
        log.error("thread {} uncaughtException", t.getName(), e);
    }
}
