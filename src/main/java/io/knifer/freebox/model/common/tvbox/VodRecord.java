package io.knifer.freebox.model.common.tvbox;

import lombok.Data;

import java.io.Serializable;

/**
 * @author pj567, Knifer
 */
@Data
public class VodRecord implements Serializable {
    private Integer id;
    private String vodId;
    private Long updateTime;
    private String sourceKey;
    private String dataJson;
}