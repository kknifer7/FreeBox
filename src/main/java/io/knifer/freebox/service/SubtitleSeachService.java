package io.knifer.freebox.service;

import cn.hutool.core.text.StrPool;
import io.knifer.freebox.model.domain.SubtitleInfo;
import io.knifer.freebox.model.domain.SubtitleSearchResponse;
import io.knifer.freebox.util.HttpUtil;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 字幕信息搜索服务
 *
 * @author Knifer
 */
@Slf4j
@Setter
public class SubtitleSeachService extends Service<SubtitleSearchResponse> {

    private String keyword;
    private Integer page;
    private boolean sortByRank = true;

    private final static String URL = "https://secure.assrt.net";
    private final static String SEARCH_URL = URL + "/sub/?searchword=%s&sort=%s&page=%d&no_redir=1";

    @Override
    protected Task<SubtitleSearchResponse> createTask() {
        return new Task<>() {
            @Override
            protected SubtitleSearchResponse call() {
                String html;

                try {
                    html = HttpUtil.getAsync(String.format(
                            SEARCH_URL, keyword, sortByRank ? "rank" : "relevance", page
                            ))
                            .get(10, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.warn("subtitle info search service exception", e);

                    return SubtitleSearchResponse.empty();
                }
                if (isCancelled() || StringUtils.isBlank(html)) {

                    return SubtitleSearchResponse.empty();
                }

                return parseResponse(html);
            }
        };
    }

    private SubtitleSearchResponse parseResponse(String html) {
        Document doc = Jsoup.parse(html);
        Elements items = doc.select(".resultcard .sublist_box_title a.introtitle");
        List<SubtitleInfo> subtitleInfoList;
        Element paginationElm;
        int page;
        int totalPage;
        Element pageElm;

        if (items.isEmpty()) {

            return SubtitleSearchResponse.empty();
        }
        subtitleInfoList = items.stream()
                .filter(item -> {
                    String href = item.attr("href");

                    return StringUtils.isNotBlank(href);
                })
                .map(item -> {
                    String title = item.attr("title");
                    String href = item.attr("href");

                    return SubtitleInfo.of(title, URL + href, true);
                })
                .toList();
        paginationElm = doc.selectFirst(".pagelinkcard");
        if (paginationElm == null) {
            page = 1;
            totalPage = 1;
        } else {
            pageElm = paginationElm.selectFirst("#pl-current");
            if (pageElm == null) {
                page = 1;
            } else {
                page = NumberUtils.toInt(pageElm.text(), 1);
            }
            pageElm = paginationElm.children().last();
            if (pageElm == null) {
                totalPage = 1;
            } else {
                totalPage = NumberUtils.toInt(
                        StringUtils.substringAfter(pageElm.text(), StrPool.SLASH), 1
                );
            }
        }

        return SubtitleSearchResponse.of(page, totalPage, subtitleInfoList);
    }
}
