package io.knifer.freebox.controller;

import io.knifer.freebox.util.CastUtil;
import lombok.Setter;

/**
 * 基础控制器
 *
 * @author Knifer
 */
@Setter
public abstract class BaseController {

    private Object data;

    public <T> T getData() {
        return CastUtil.cast(data);
    }
}
