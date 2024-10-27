package io.knifer.freebox.model.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 收藏影片
 *
 * @author Cuke
 */
@Data
public class VodCollect implements Serializable {
    private int id;
    public String vodId;
    public long updateTime;
    public String sourceKey;
    public String name;
    public String pic;
}