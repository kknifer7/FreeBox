package io.knifer.freebox.ioc;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.stage.Stage;
import lombok.experimental.UtilityClass;

/**
 * IOC容器工具类
 *
 * @author Knifer
 */
@UtilityClass
public class IOC {

    private Injector injector;

    private volatile boolean initFlag = false;

    public void init(Stage primaryStage) {
        if (initFlag) {
            throw new AssertionError();
        }
        injector = Guice.createInjector(binder ->
            binder.bind(Stage.class).toInstance(primaryStage)
        );
        initFlag = true;
    }

    public <T> T getBean(Class<T> clazz) {
        return injector.getInstance(clazz);
    }
}
