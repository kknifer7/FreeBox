package io.knifer.freebox.model.domain;

import lombok.Data;

/**
 * 本地设置
 *
 * @author Knifer
 */
@Data
public class Config {

    /**
     * 唯一标识
     */
    private String uuid;

    /**
     * 源地址
     */
    private String sourceLink;
}
