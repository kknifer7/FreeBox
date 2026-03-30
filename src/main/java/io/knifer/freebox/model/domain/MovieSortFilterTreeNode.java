package io.knifer.freebox.model.domain;

import io.knifer.freebox.model.common.tvbox.MovieSort;
import lombok.Data;

import java.util.Map;

/**
 * 影视浏览筛选条件树状数据项
 *
 * @author Knifer
 */
@Data
public class MovieSortFilterTreeNode {

    private String filterKey;
    private String filterName;
    private String filterValueName;
    private String filterValue;
    /**
     * 是否为filter里面的value（如果不是，就是外层的filter）
     */
    private Boolean filterValueFlag;

    public static MovieSortFilterTreeNode from(MovieSort.SortFilter sortDataFilter) {
        MovieSortFilterTreeNode item = new MovieSortFilterTreeNode();

        item.setFilterValueFlag(false);
        item.setFilterKey(sortDataFilter.getKey());
        item.setFilterName(sortDataFilter.getName());

        return item;
    }

    public static MovieSortFilterTreeNode from(Map.Entry<String, String> filterValueNameAndValue) {
        MovieSortFilterTreeNode item = new MovieSortFilterTreeNode();

        item.setFilterValueFlag(true);
        item.setFilterValueName(filterValueNameAndValue.getKey());
        item.setFilterValue(filterValueNameAndValue.getValue());

        return item;
    }
}
