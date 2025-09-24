package io.knifer.freebox.model.common.diyp;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 电子节目单数据
 *
 * @author Knifer
 */
@Data
public class EPG {

    /**
     * 频道名称
     */
    @SerializedName("channel_name")
    private String channelName;

    /**
     * 日期 eg. 2020-01-01
     */
    private String date;

    /**
     * 节目数据
     */
    @SerializedName("epg_data")
    private List<Data> epgData;

    @AllArgsConstructor
    @lombok.Data
    public static class Data {

        /**
         * 开始时间 eg. 12:00
         */
        private String start;

        /**
         * 结束时间 eg. 13:00
         */
        private String end;

        /**
         * 节目名称
         */
        private String title;

        /**
         * 节目描述
         */
        private String desc;
    }
}
