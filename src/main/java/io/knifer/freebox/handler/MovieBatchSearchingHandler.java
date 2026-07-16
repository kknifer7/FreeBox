package io.knifer.freebox.handler;

import com.google.inject.ImplementedBy;
import io.knifer.freebox.handler.impl.CommonMovieBatchSearchingHandler;
import io.knifer.freebox.model.common.tvbox.AbsXml;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * 批量调用Spider影视搜索
 * @author Knifer
 */
@ImplementedBy(CommonMovieBatchSearchingHandler.class)
public interface MovieBatchSearchingHandler {

    /**
     * 批量调用Spider影视搜索
     * @param sourceKeys 搜索源
     * @param keyword 搜索关键字
     * @param callback 单次搜索结果回调（keyword+结果）
     * @param finishCallback 搜索完成回调
     */
    void handle(
            Collection<String> sourceKeys,
            String keyword,
            Consumer<Pair<String, AbsXml>> callback,
            Runnable finishCallback
    );

    /**
     * 取消搜索
     */
    void cancelSearching(Runnable callback);
}
