package io.knifer.freebox.model.c2s;

import lombok.Data;

/**
 * 直播配置
 *
 * @author Knifer
 */
@Data
public class FreeBoxLive {

    private String name;

    private String url;

    private String ua;

    private String epg;

    private String logo;

    public static FreeBoxLive from(String url) {
        FreeBoxLive result = new FreeBoxLive();

        result.setUrl(url);

        return result;
    }
}
