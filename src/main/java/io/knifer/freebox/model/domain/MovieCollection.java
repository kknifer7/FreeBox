package io.knifer.freebox.model.domain;

import io.github.filelize.Filelize;
import io.github.filelize.FilelizeType;
import io.github.filelize.Id;
import io.knifer.freebox.model.common.tvbox.VodCollect;
import lombok.Data;

import java.util.Map;

/**
 * 影片收藏数据
 *
 * @author Knifer
 */
@Data
@Filelize(name = "movie_collection", type = FilelizeType.MULTIPLE_FILES, directory = "movie_collection")
public class MovieCollection implements Savable {

    /**
     * ID
     */
    @Id
    private String id;

    /**
     * 数据（key=md5(sourceKey+vodId), value=VodInfo）
     */
    private Map<String, VodCollect> data;

    public static MovieCollection of(String id, Map<String, VodCollect> data) {
        MovieCollection result = new MovieCollection();

        result.id = id;
        result.data = data;

        return result;
    }
}
