package io.knifer.freebox.handler;

import com.google.inject.ImplementedBy;
import io.knifer.freebox.handler.impl.ParameterizedEpgFetchingHandler;
import io.knifer.freebox.model.common.diyp.EPG;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * 获取电子节目单
 */
@ImplementedBy(ParameterizedEpgFetchingHandler.class)
public interface EpgFetchingHandler {

    CompletableFuture<EPG> handle(String epgServiceUrl, String channelName, LocalDate date);
}
