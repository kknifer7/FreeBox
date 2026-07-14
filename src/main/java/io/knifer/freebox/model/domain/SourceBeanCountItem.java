package io.knifer.freebox.model.domain;

import io.knifer.freebox.model.common.tvbox.SourceBean;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 源站点及其搜索结果计数
 *
 * @author Knifer
 */
@RequiredArgsConstructor
public class SourceBeanCountItem {

    @Getter
    private final SourceBean sourceBean;

    private final IntegerProperty movieCount = new SimpleIntegerProperty();

    public SourceBeanCountItem(SourceBean sourceBean, int movieCount) {
        this.sourceBean = sourceBean;
        this.movieCount.set(movieCount);
    }

    public int getMovieCount() {
        return movieCount.get();
    }

    public void setMovieCount(int movieCount) {
        this.movieCount.set(movieCount);
    }

    public IntegerProperty movieCountProperty() {
        return movieCount;
    }
}
