package io.knifer.freebox.component.event;

import com.google.common.eventbus.Subscribe;

/**
 * Guava EventBus 事件监听器
 *
 * @author Knifer
 */
@FunctionalInterface
public interface EventListener<T> {

    @Subscribe
    void apply(T evt);
}
