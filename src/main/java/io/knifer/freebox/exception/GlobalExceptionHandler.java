package io.knifer.freebox.exception;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.ToastHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理
 *
 * @author Knifer
 */
@Slf4j
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (Context.INSTANCE.isInitialized()) {
            ToastHelper.showException(e);
        }
        log.error("thread {} uncaughtException", t.getName(), e);
    }
}
