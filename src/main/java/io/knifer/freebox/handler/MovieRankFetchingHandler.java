package io.knifer.freebox.handler;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 获取影视热搜处理器
 *
 * @author knifer
 */
public interface MovieRankFetchingHandler {

    @NotNull
    List<String> handle();
}
