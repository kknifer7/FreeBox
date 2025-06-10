package io.knifer.freebox.model.domain;

import io.github.filelize.Filelize;
import io.github.filelize.FilelizeType;
import io.github.filelize.Id;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import lombok.Data;

import java.util.List;

/**
 * 源屏蔽列表
 *
 * @author Knifer
 */
@Data
@Filelize(name = "source_bean_block_list", type = FilelizeType.MULTIPLE_FILES, directory = "source_bean_block_list")
public class SourceBeanBlockList implements Savable {

    @Id
    private String id;

    private List<String> sourceBeanKeys;

    public static SourceBeanBlockList of(String id, List<SourceBean> sourceBeans) {
        SourceBeanBlockList sourceBeanBlockList = new SourceBeanBlockList();

        sourceBeanBlockList.id = id;
        sourceBeanBlockList.sourceBeanKeys = sourceBeans.stream().map(SourceBean::getKey).toList();

        return sourceBeanBlockList;
    }
}
