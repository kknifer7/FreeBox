package io.knifer.freebox.model.s2c;

import io.knifer.freebox.model.common.tvbox.MovieSort;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import lombok.Data;

/**
 * 获取指定分类信息
 * @author knifer
 */
@Data
public class GetCategoryContentDTO {
    
    private SourceBean source;
    
    private MovieSort.SortData sortData;
    
    private Integer page;

    public static GetCategoryContentDTO of(SourceBean source, MovieSort.SortData sortData, Integer page) {
        GetCategoryContentDTO dto = new GetCategoryContentDTO();

        dto.setSource(source);
        dto.setSortData(sortData);
        dto.setPage(page);

        return dto;
    }
}