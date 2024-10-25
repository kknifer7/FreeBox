package io.knifer.freebox.model.s2c;

import lombok.Data;

/**
 * 影视搜索
 *
 * @author Knifer
 */
@Data
public class GetSearchContentDTO {

    /**
     * 源键值
     */
    private String sourceKey;

    /**
     * 关键字
     */
    private String keyword;

    public static GetSearchContentDTO of(String sourceKey, String keyword) {
        GetSearchContentDTO result = new GetSearchContentDTO();

        result.setSourceKey(sourceKey);
        result.setKeyword(keyword);

        return result;
    }
}
