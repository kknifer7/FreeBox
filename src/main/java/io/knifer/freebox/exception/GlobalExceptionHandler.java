package io.knifer.freebox.exception;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.ToastHelper;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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
//        if (shouldIgnore(e)) {
//            return;
//        }
        if (Context.INSTANCE.isInitialized()) {
            if (Platform.isFxApplicationThread()) {
                ToastHelper.showException(e);
            } else {
                Platform.runLater(() -> ToastHelper.showException(e));
            }
        }
        log.error("thread {} uncaughtException", t.getName(), e);
    }

    private boolean shouldIgnore(Throwable e) {
        StackTraceElement stElm = e.getStackTrace()[0];

        // okhttp 库固定抛出的异常，忽略
        return e instanceof IllegalArgumentException &&
                StringUtils.equals(stElm.getFileName(), "HttpUrl.kt") &&
                StringUtils.equals(stElm.getClassName(), "okhttp3.HttpUrl$Builder") &&
                StringUtils.equals(stElm.getMethodName(), "parse$okhttp") &&
                stElm.getLineNumber() == 1441;
    }
}
