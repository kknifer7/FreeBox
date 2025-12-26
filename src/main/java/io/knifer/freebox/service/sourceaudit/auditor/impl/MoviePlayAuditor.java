package io.knifer.freebox.service.sourceaudit.auditor.impl;

import com.google.gson.JsonElement;
import io.knifer.freebox.constant.SourceAuditResult;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.model.s2c.GetPlayerContentDTO;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.service.sourceaudit.SourceAuditContext;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditor;
import io.knifer.freebox.util.json.GsonUtil;
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

    private final static String[] VIDEO_URL_PREFIX = {
            "http",
            "magnet",
            "data:"
    };

    public MoviePlayAuditor(SpiderTemplate spiderTemplate) {
        super(spiderTemplate);
    }

    @Override
    public boolean support(SourceAuditType sourceAuditType) {
        return sourceAuditType == SourceAuditType.MOVIE_PLAY;
    }

    @Override
    public void audit(SourceAuditContext context, boolean skip) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();

        if (skip || context.isInterrupt()) {
            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_PLAY, SourceAuditStatus.SKIPPED));
            onFinish.accept(Pair.of(SourceAuditType.MOVIE_PLAY, List.of()));
            doNext(context, true);

            return;
        }
        onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_PLAY, SourceAuditStatus.PROCESSING));
        doAudit(context, 0);
    }

    private void doAudit(SourceAuditContext context, int retryCount) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();
        Consumer<Pair<SourceAuditType, String>> onRequest = context.getOnRequest();
        Consumer<Pair<SourceAuditType, String>> onResponse = context.getOnResponse();
        Movie.Video video = context.getDetailContent().getMovie().getVideoList().get(0);
        Movie.Video.UrlBean.UrlInfo urlInfo = video.getUrlBean().getInfoList().get(0);
        GetPlayerContentDTO dto = GetPlayerContentDTO.of(
                video.getSourceKey(),
                urlInfo.getFlag(),
                urlInfo.getBeanList().get(0).getUrl()
        );
        int maxRetryCount = context.getMaxRetryCount();

        onRequest.accept(Pair.of(SourceAuditType.MOVIE_PLAY, GsonUtil.toPrettyJson(dto)));
        spiderTemplate.getPlayerContent(
                dto,
                playerContentJson -> {
                    JsonElement propElm;
                    JsonElement urlElem;
                    boolean needSkip;

                    if (playerContentJson == null) {
                        if (retryCount >= maxRetryCount) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_PLAY, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(SourceAuditType.MOVIE_PLAY, List.of(SourceAuditResult.NO_DATA)));
                            needSkip = true;
                        } else {
                            doAudit(context, retryCount + 1);

                            return;
                        }
                    } else {
                        onResponse.accept(Pair.of(
                                SourceAuditType.MOVIE_PLAY, GsonUtil.toPrettyJson(playerContentJson)
                        ));
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
                        } else if (!StringUtils.startsWithAny(urlElem.getAsString(), VIDEO_URL_PREFIX)) {
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
