package io.knifer.freebox.model.domain;

import cn.hutool.json.JSONUtil;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import lombok.Data;

/**
 * FreeBox接收的源对象
 *
 * @author Knifer
 */
@Data
public class FreeBoxSourceBean {

    private String key;
    private String name;
    private String api;
    private int type;   // 0 xml 1 json 3 Spider
    private int searchable; // 是否可搜索
    private int quickSearch; // 是否可以快速搜索
    private int filterable; // 可筛选?
    private Object ext; // 扩展字段

    public SourceBean toSourceBean() {
        SourceBean sourceBean = new SourceBean();

        sourceBean.setKey(this.key);
        sourceBean.setName(this.name);
        sourceBean.setApi(this.api);
        sourceBean.setType(this.type);
        sourceBean.setSearchable(this.searchable);
        sourceBean.setQuickSearch(this.quickSearch);
        sourceBean.setFilterable(this.filterable);
        if (this.ext != null) {
            sourceBean.setExt(JSONUtil.toJsonStr(this.ext));
        }

        return sourceBean;
    }
}
