package io.knifer.freebox.controller;

import io.knifer.freebox.util.CastUtil;

/**
 * 基础控制器
 *
 * @author Knifer
 */
public abstract class BaseController {

    private Object data;

    public void setData(Object data) {
        this.data = data;
    }

    public <T> T getData() {
        return CastUtil.cast(data);
    }
}
