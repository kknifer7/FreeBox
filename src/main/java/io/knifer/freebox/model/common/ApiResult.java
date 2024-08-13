package io.knifer.freebox.model.common;

import io.knifer.freebox.constant.ResultCodes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API返回
 *
 * @author Knifer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private Integer code;

    private String msg;

    private T data;

    public static <T> ApiResult<T> ok(T data) {
        return of(ResultCodes.SUCCESS, null, data);
    }

    public static<T> ApiResult<T> of(Integer code, String msg, T data) {
        return new ApiResult<T>(code, msg, data);
    }
}
