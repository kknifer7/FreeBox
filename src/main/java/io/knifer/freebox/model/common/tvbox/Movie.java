package io.knifer.freebox.model.common.tvbox;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author pj567
 */
@Data
public class Movie implements Serializable {
    public int page;
    public int pagecount;//总页数
    public int pagesize;
    public int recordcount;//总条数
    public List<Video> videoList;

    @Data
    public static class Video implements Serializable {
        public String last;
        public String id;
        public int tid;
        public String name;
        public String type;
        public String pic;
        public String lang;
        public String area;
        public int year;
        public String state;
        public String note;
        public String actor;
        public String director;
        public UrlBean urlBean;
        public String des;// <![CDATA[权来]
        public String sourceKey;
        public String tag;

        @Data
        public static class UrlBean implements Serializable {
            public List<UrlInfo> infoList;

            @Data
            public static class UrlInfo implements Serializable {
                public String flag;
                public String urls;
                public List<InfoBean> beanList;

                @Data
                public static class InfoBean implements Serializable {
                    public String name;
                    public String url;

                    public InfoBean(String name, String url) {
                        this.name = name;
                        this.url = url;
                    }

                }
            }

        }

    }

}