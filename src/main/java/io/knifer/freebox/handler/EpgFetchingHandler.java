package io.knifer.freebox.handler;

import io.knifer.freebox.model.common.diyp.Epg;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * 获取电子节目单
 */
public interface EpgFetchingHandler {

    CompletableFuture<Epg> handle(String epgServiceUrl, String channelName, LocalDate date);
}
