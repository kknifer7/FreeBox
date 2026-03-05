package io.knifer.freebox.spider.js;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;

import java.util.function.Consumer;

/**
 * 处理JS中的Promise对象简易封装
 *
 * @author Knifer
 */
@Slf4j
public class Promise<T> {

    private final Value raw;
    private final Class<T> resultType;
    private final boolean available;

    private Promise(Value raw, Class<T> resultType) {
        this.raw = raw;
        this.resultType = resultType;
        this.available = raw.canInvokeMember("then") && raw.canInvokeMember("catch");
    }

    public Promise<T> then(Consumer<T> resolve) {
        if (available) {
            raw.invokeMember("then", resolve);
        } else {
            logUnavailable("resolve");
            resolve.accept(null);
        }

        return this;
    }

    public Promise<T> catchError(Consumer<Object> reject) {
        if (available) {
            raw.invokeMember("catch", reject);
        } else {
            logUnavailable("catchError");
        }

        return this;
    }

    private void logUnavailable(String operation) {
        log.error("{} failed, promise unavailable. raw={}, resultType={}", operation, raw, resultType.getTypeName());
    }

    public static <T> Promise<T> of(Value raw, Class<T> resultType) {
        return new Promise<>(raw, resultType);
    }
}
