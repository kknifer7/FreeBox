package io.knifer.freebox.service.sourceaudit.auditor.impl;

import com.google.gson.JsonElement;
import io.knifer.freebox.constant.SourceAuditResult;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.model.common.Movie;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.s2c.GetPlayerContentDTO;
import io.knifer.freebox.service.sourceaudit.SourceAuditContext;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditor;
import io.knifer.freebox.util.GsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Consumer;

/**
 * 影片播放审计
 *
 * @author Knifer
 */
public class MoviePlayAuditor extends SourceAuditor {
    @Override
    public boolean support(SourceAuditType sourceAuditType) {
        return sourceAuditType == SourceAuditType.MOVIE_PLAY;
    }

    @Override
    public void audit(SourceAuditContext context, boolean skip) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();
        GetPlayerContentDTO dto;
        Consumer<Pair<SourceAuditType, String>> onRequest;
        Consumer<Pair<SourceAuditType, String>> onResponse;
        ClientInfo clientInfo;
        Movie.Video video;
        Movie.Video.UrlBean.UrlInfo urlInfo;

        if (skip) {
            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_PLAY, SourceAuditStatus.SKIPPED));
            onFinish.accept(Pair.of(SourceAuditType.MOVIE_PLAY, List.of()));
            doNext(context, true);

            return;
        }
        onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_PLAY, SourceAuditStatus.PROCESSING));
        clientInfo = context.getClientInfo();
        video = context.getDetailContent().getMovie().getVideoList().get(0);
        urlInfo = video.getUrlBean().getInfoList().get(0);
        dto = GetPlayerContentDTO.of(
                video.getSourceKey(),
                StringUtils.EMPTY,
                urlInfo.getFlag(),
                urlInfo.getBeanList().get(0).getUrl()
        );
        onRequest = context.getOnRequest();
        onResponse = context.getOnResponse();
        onRequest.accept(Pair.of(SourceAuditType.MOVIE_PLAY, GsonUtil.toPrettyJson(dto)));
        kebSocketTemplate.getPlayerContent(
                clientInfo,
                dto,
                playerContentJson -> {
                    JsonElement propElm;
                    JsonElement urlElem;
                    boolean needSkip;

                    if (playerContentJson == null) {
                        onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_PLAY, SourceAuditStatus.FAILED));
                        onFinish.accept(Pair.of(SourceAuditType.MOVIE_PLAY, List.of(SourceAuditResult.NO_DATA)));
                        doNext(context, true);

                        return;
                    } else {
                        onResponse.accept(Pair.of(SourceAuditType.MOVIE_PLAY, GsonUtil.toPrettyJson(playerContentJson)));
                        if (
                                (propElm = playerContentJson.get("nameValuePairs")) == null ||
                                !propElm.isJsonObject() ||
                                (urlElem = propElm.getAsJsonObject().get("url")) == null
                        ) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_PLAY, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(
                                    SourceAuditType.MOVIE_PLAY, List.of(SourceAuditResult.NO_VIDEO_URL)
                            ));
                            needSkip = true;
                        } else if (!urlElem.getAsString().startsWith("http")) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_PLAY, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(
                                    SourceAuditType.MOVIE_PLAY, List.of(SourceAuditResult.INVALID_VIDEO_URL)
                            ));
                            needSkip = true;
                        } else {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_PLAY, SourceAuditStatus.SUCCESS));
                            onFinish.accept(Pair.of(SourceAuditType.MOVIE_PLAY, List.of()));
                            needSkip = false;
                        }
                    }
                    doNext(context, needSkip);
                }
        );
    }
}
