package io.knifer.freebox.component.converter;

import com.google.common.base.Strings;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;

/**
 * 源对象转字符串转换器
 *
 * @author Knifer
 */
public class SourceBean2StringConverter extends StringConverter<SourceBean> {
    @Override
    public String toString(SourceBean sourceBean) {
        return sourceBean == null ? StringUtils.EMPTY : Strings.nullToEmpty(sourceBean.getName());
    }

    @Override
    public SourceBean fromString(String string) {
        throw new UnsupportedOperationException();
    }
}
