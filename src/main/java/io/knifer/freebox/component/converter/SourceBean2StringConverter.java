package io.knifer.freebox.component.converter;

import io.knifer.freebox.model.common.SourceBean;
import javafx.util.StringConverter;

/**
 * 源对象转字符串转换器
 *
 * @author Knifer
 */
public class SourceBean2StringConverter extends StringConverter<SourceBean> {
    @Override
    public String toString(SourceBean sourceBean) {
        return sourceBean.getName();
    }

    @Override
    public SourceBean fromString(String string) {
        throw new UnsupportedOperationException();
    }
}
