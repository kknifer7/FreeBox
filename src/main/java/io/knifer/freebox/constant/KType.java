package io.knifer.freebox.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TVBox远程客户端的细分类型
 */
@Getter
@AllArgsConstructor
public enum KType implements ValueEnum<Integer> {

    /**
     * FongMi TV
     */
    TV(0);

    private final Integer value;
}
