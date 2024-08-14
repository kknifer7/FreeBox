package io.knifer.freebox.context;

import javafx.application.Application;

/**
 * 全局上下文
 *
 * @author Knifer
 */
public enum Context {

    INSTANCE;

    private Application app;

    private volatile boolean initFlag = false;

    private void setApp(Application app) {
        if (this.app == null) {
            this.app = app;
        } else {
            throw new IllegalStateException("app has already been set");
        }
    }

    public Application getApp() {
        if (app == null) {
            throw new IllegalStateException("application has not started yet");
        }

        return app;
    }

    public void init(Application app) {
        setApp(app);
        initFlag = true;
    }

    public boolean isInitialized() {
        return initFlag;
    }
}
