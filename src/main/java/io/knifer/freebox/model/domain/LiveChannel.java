package io.knifer.freebox.model.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播频道
 *
 * @author Knifer
 */
@Data
public class LiveChannel {

    /**
     * 频道组标题（作为频道的分组依据）
     */
    private String groupTitle;

    /**
     * 频道标题（作为区分频道多线路的依据）
     */
    private String title;

    /**
     * 频道地址（可多线路）
     */
    private List<Line> lines;

    public static LiveChannel of(String groupTitle, String title) {
        LiveChannel result = new LiveChannel();

        result.setGroupTitle(groupTitle);
        result.setTitle(title);
        result.setLines(new ArrayList<>());

        return result;
    }

    @Data
    public static class Line {

        /**
         * 线路标题
         */
        private String title;

        /**
         * 线路图标
         */
        private String logoUrl;

        /**
         * 线路地址
         */
        private String url;

        public static Line of(String title, String logoUrl, String url) {
            Line result = new Line();

            result.setTitle(title);
            result.setLogoUrl(logoUrl);
            result.setUrl(url);

            return result;
        }
    }
}
