package io.knifer.freebox.service;

import io.knifer.freebox.model.domain.SubtitleInfo;
import io.knifer.freebox.util.HttpUtil;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import kotlin.text.Charsets;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字幕获取服务
 *
 * @author Knifer
 */
@Slf4j
@Setter
public class SubtitleFetchService extends Service<List<SubtitleInfo>> {

    private String detailUrl;

    private final static String URL = "https://secure.assrt.net";
    private final static Pattern regexShooterFileOnclick =
            Pattern.compile("onthefly\\(\"(\\d+)\",\"(\\d+)\",\"([\\s\\S]*)\"\\)");

    @Override
    protected Task<List<SubtitleInfo>> createTask() {
        return new Task<>() {
            @Override
            protected List<SubtitleInfo> call() {
                String html;

                try {
                    html = HttpUtil.getAsync(detailUrl).get(10, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.warn("subtitle fetch service exception", e);

                    return List.of();
                }
                if (isCancelled() || StringUtils.isBlank(html)) {

                    return List.of();
                }

                return parseSubtitles(html);
            }
        };
    }

    private List<SubtitleInfo> parseSubtitles(String html) {
        Document doc = Jsoup.parse(html);
        Elements items = doc.select("#detail-filelist .waves-effect");
        List<SubtitleInfo> subtitles;
        String onclick;
        Matcher matcher;
        Element elm;
        String downloadHref;
        String downloadUrl;
        String downloadUrlLowerCase;
        String title;

        if (items.isEmpty()) {
            // 单独的字幕文件
            elm = doc.selectFirst("#btn_download");
            if (elm == null) {

                return List.of();
            }
            downloadHref = elm.attr("href");
            if (StringUtils.isBlank(downloadHref)) {

                return List.of();
            }
            downloadUrlLowerCase = downloadHref.toLowerCase();
            if (
                    downloadUrlLowerCase.endsWith("srt") ||
                    downloadUrlLowerCase.endsWith("ass") ||
                    downloadUrlLowerCase.endsWith("scc") ||
                    downloadUrlLowerCase.endsWith("ttml") ||
                    downloadUrlLowerCase.endsWith("vtt") ||
                    downloadUrlLowerCase.endsWith("ssa")
            ) {
                downloadUrl = URL + downloadHref;
                title = URLDecoder.decode(
                        downloadHref.substring(downloadHref.lastIndexOf("/") + 1), Charsets.UTF_8
                );

                return List.of(SubtitleInfo.of(title, downloadUrl, false));
            } else {

                return List.of();
            }
        } else {
            // 字幕文件压缩包
            subtitles = new ArrayList<>(items.size());
            for (Element item : items) {
                onclick = item.attr("onclick");
                matcher = regexShooterFileOnclick.matcher(onclick);
                if (matcher.find()) {
                    elm = item.selectFirst("#filelist-name");
                    downloadUrl = String.format(
                            URL + "/download/%s/-/%s/%s",
                            matcher.group(1),
                            matcher.group(2),
                            matcher.group(3)
                    );
                    subtitles.add(SubtitleInfo.of(
                            elm == null ? matcher.group(3) : elm.text(), downloadUrl, false
                    ));
                }
            }
        }

        return subtitles;
    }
}
