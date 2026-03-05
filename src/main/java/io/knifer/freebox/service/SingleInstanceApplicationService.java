package io.knifer.freebox.service;

import io.knifer.freebox.handler.impl.SingleInstanceApplicationHandler;
import io.knifer.freebox.ioc.IOC;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 * 用于确保应用单例的服务
 *
 * @author Knifer
 */
public class SingleInstanceApplicationService extends Service<Void> {

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                IOC.getBean(SingleInstanceApplicationHandler.class).handle();

                return null;
            }
        };
    }
}
