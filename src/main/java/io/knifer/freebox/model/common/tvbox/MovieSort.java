package io.knifer.freebox.model.common.tvbox;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author pj567
 */
@Data
public class MovieSort implements Serializable {
    public List<SortData> sortList;

    @Data
    public static class SortData implements Serializable, Comparable<SortData> {
        public String id;
        public String name;
        public int sort = -1;
        public boolean select = false;
        public ArrayList<SortFilter> filters = new ArrayList<>();
        public HashMap<String, String> filterSelect = new HashMap<>();
        public String flag; // 类型

        public SortData() {
        }

        public SortData(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public int filterSelectCount() {
            if (filterSelect == null) {
                return 0;
            }
            int count = 0;
            for (String filter : filterSelect.values()) {
                if (filter != null && !filter.isEmpty()) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public int compareTo(SortData o) {
            return this.sort - o.sort;
        }

        @Override
        public String toString() {
            return "SortData{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", sort=" + sort +
                    ", select=" + select +
                    ", filters=" + filters +
                    ", filterSelect=" + filterSelect +
                    ", flag='" + flag + '\'' +
                    '}';
        }
    }

    @Data
    public static class SortFilter {
        public String key;
        public String name;
        public LinkedHashMap<String, String> values;

        @Override
        public String toString() {
            return "SortFilter{" +
                    "key='" + key + '\'' +
                    ", name='" + name + '\'' +
                    ", values=" + values +
                    '}';
        }
    }

}