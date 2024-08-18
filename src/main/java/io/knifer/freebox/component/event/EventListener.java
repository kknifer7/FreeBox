package io.knifer.freebox.component.event;

import com.google.common.eventbus.Subscribe;

/**
 * Guava EventBus 事件监听器
 * @see com.google.common.eventbus.EventBus
 * 为了书写方便，这里使用了泛型，但因为Java运行时的泛型擦除，这样写会导致Guava向所有类型的事件监听器分发事件，
 * 进而导致事件对象的ClassCastException。
 * 目前的解决方案是自定义EventBus的异常处理逻辑，忽略所有ClassCastException异常。
 * 这样做有性能问题，但我不认为它有多大影响
 *
 * @author Knifer
 */
@FunctionalInterface
public interface EventListener<T> {

    @Subscribe
    void apply(T evt);
}
