package io.knifer.freebox.service.sourceaudit.auditor.impl;

import io.knifer.freebox.constant.SourceAuditResult;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.model.common.tvbox.AbsXml;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.model.s2c.GetDetailContentDTO;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.service.sourceaudit.SourceAuditContext;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditor;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.json.GsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Consumer;

/**
 * 影片详情信息审计
 * 
 * @author Knifer
 */
public class MovieDetailAuditor extends SourceAuditor {

    public MovieDetailAuditor(SpiderTemplate spiderTemplate) {
        super(spiderTemplate);
    }

    @Override
    public boolean support(SourceAuditType sourceAuditType) {
        return sourceAuditType == SourceAuditType.MOVIE_DETAIL;
    }

    @Override
    public void audit(SourceAuditContext context, boolean skip) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();

        if (skip || context.isInterrupt()) {
            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, SourceAuditStatus.SKIPPED));
            onFinish.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, List.of()));
            doNext(context, true);

            return;
        }
        onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, SourceAuditStatus.PROCESSING));
        doAudit(context, 0);
    }

    private void doAudit(SourceAuditContext context, int retryCount) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();
        Consumer<Pair<SourceAuditType, String>> onRequest = context.getOnRequest();
        Consumer<Pair<SourceAuditType, String>> onResponse = context.getOnResponse();
        AbsXml categoryContent = context.getCategoryContent();
        Movie.Video video = categoryContent.getMovie().getVideoList().get(0);
        GetDetailContentDTO dto = GetDetailContentDTO.of(context.getSourceBean().getKey(), video.getId());
        int maxRetryCount = context.getMaxRetryCount();

        onRequest.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, GsonUtil.toPrettyJson(dto)));
        spiderTemplate.getDetailContent(
                dto,
                detailContent -> {
                    boolean needSkip;
                    Movie movie;
                    List<Movie.Video> videos;
                    Movie.Video v;
                    Movie.Video.UrlBean urlBean;

                    if (detailContent == null) {
                        if (retryCount >= maxRetryCount) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, List.of(SourceAuditResult.NO_DATA)));
                            needSkip = true;
                        } else {
                            doAudit(context, retryCount + 1);

                            return;
                        }
                    } else {
                        onResponse.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, GsonUtil.toPrettyJson(detailContent)));
                        context.setDetailContent(detailContent);
                        movie = detailContent.getMovie();
                        if (movie == null || (CollectionUtil.isEmpty(videos = movie.getVideoList()))) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(
                                    SourceAuditType.MOVIE_DETAIL, List.of(SourceAuditResult.NO_VIDEO_LIST_ERROR)
                            ));
                            needSkip = true;
                        } else if ((v = videos.get(0)) == null) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(
                                    SourceAuditType.MOVIE_DETAIL, List.of(SourceAuditResult.NO_VIDEO)
                            ));
                            needSkip = true;
                        } else if (StringUtils.isBlank(v.getId())) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(
                                    SourceAuditType.MOVIE_DETAIL, List.of(SourceAuditResult.INVALID_VIDEO)
                            ));
                            needSkip = true;
                        } else if (
                                (urlBean = v.getUrlBean()) == null ||
                                        CollectionUtil.isEmpty(urlBean.getInfoList()) ||
                                        CollectionUtil.isEmpty(urlBean.getInfoList().get(0).getBeanList())
                        ) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(
                                    SourceAuditType.MOVIE_DETAIL, List.of(SourceAuditResult.INVALID_VIDEO_URLS)
                            ));
                            needSkip = true;
                        } else {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, SourceAuditStatus.SUCCESS));
                            onFinish.accept(Pair.of(SourceAuditType.MOVIE_DETAIL, List.of()));
                            needSkip = false;
                        }
                    }
                    doNext(context, needSkip);
                }
        );
    }
}
