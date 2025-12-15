package io.knifer.freebox.model.s2c;

import lombok.Data;

import java.util.HashMap;

/**
 * 获取指定分类信息
 * @author knifer
 */
@Data
public class GetCategoryContentDTO {

    /**
     * 站点key
     */
    private String sourceKey;

    /**
     * 分类id
     */
    private String tid;

    /**
     * 是否过滤
     */
    private boolean filter;

    /**
     * 页数
     */
    private String page;

    /**
     * 筛选参数
     */
    private HashMap<String, String> extend;

    public static GetCategoryContentDTO of(
            String sourceKey,
            String tid,
            boolean filter,
            String page,
            HashMap<String, String> extend
    ) {
        GetCategoryContentDTO dto = new GetCategoryContentDTO();

        dto.setSourceKey(sourceKey);
        dto.setTid(tid);
        dto.setFilter(filter);
        dto.setPage(page);
        dto.setExtend(extend);

        return dto;
    }
}