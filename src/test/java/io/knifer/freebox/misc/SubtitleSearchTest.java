package io.knifer.freebox.misc;

import cn.hutool.core.net.URLEncodeUtil;
import com.google.common.net.HttpHeaders;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.model.domain.SubtitleInfo;
import io.knifer.freebox.util.HttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Knifer
 */
public class SubtitleSearchTest {

    @Test
    void test() throws ExecutionException, InterruptedException {
        String url = "https://assrt.net";
        String searchUrl = String.format(url + "/sub/?searchword=%s&sort=rank&page=%d&no_redir=1", "行尸走肉第一季", 1);
        String html = HttpUtil.getAsync(searchUrl).get();
        Document doc = Jsoup.parse(html);
        Elements items = doc.select(".resultcard .sublist_box_title a.introtitle");
        List<SubtitleInfo> subtitleInfoList = items.stream()
                .filter(item -> {
                    String href = item.attr("href");

                    return StringUtils.isNotBlank(href);
                })
                .map(item -> {
                    String title = item.attr("title");
                    String href = item.attr("href");

                    return SubtitleInfo.of(title, url + href, true);
                })
                .toList();
        SubtitleInfo subtitleInfo = subtitleInfoList.get(0);
        String detailUrl = subtitleInfo.getUrl();
        Pattern regexShooterFileOnclick = Pattern.compile("onthefly\\(\"(\\d+)\",\"(\\d+)\",\"([\\s\\S]*)\"\\)");
        String onclick;
        Matcher matcher;
        Element elm;
        String downloadUrl;
        List<SubtitleInfo> subtitles;

        html = HttpUtil.getAsync(detailUrl, "Referer", URLEncodeUtil.encode(searchUrl), HttpHeaders.USER_AGENT, BaseValues.USER_AGENT).get();
        doc = Jsoup.parse(html);
        items = doc.select("#detail-filelist .waves-effect");
        subtitles = new ArrayList<>(items.size());
        for (Element item : items) {
            onclick = item.attr("onclick");
            matcher = regexShooterFileOnclick.matcher(onclick);
            if (matcher.find()) {
                elm = item.selectFirst("#filelist-name");
                downloadUrl = String.format(
                        "https://secure.assrt.net/download/%s/-/%s/%s",
                        matcher.group(1),
                        matcher.group(2),
                        matcher.group(3)
                );
                subtitles.add(SubtitleInfo.of(
                        elm == null ? matcher.group(3) : elm.text(), downloadUrl, false
                ));
            }
        }
        System.out.println(subtitles);
    }
}
