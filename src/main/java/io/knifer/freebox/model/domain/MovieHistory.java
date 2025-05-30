package io.knifer.freebox.model.domain;

import io.github.filelize.Filelize;
import io.github.filelize.FilelizeType;
import io.github.filelize.Id;
import io.knifer.freebox.model.common.tvbox.VodInfo;
import lombok.Data;

import java.util.Map;

/**
 * 影片历史数据
 *
 * @author Knifer
 */
@Data
@Filelize(name = "movie_history", type = FilelizeType.MULTIPLE_FILES, directory = "movie_history")
public class MovieHistory implements Savable {

    /**
     * ID
     */
    @Id
    private String clientId;

    /**
     * 数据（key=md5(sourceKey+vodId), value=VodInfo）
     */
    private Map<String, VodInfo> data;

    @Override
    public String getId() {
        return clientId;
    }

    public static MovieHistory of(String clientId, Map<String, VodInfo> data) {
        MovieHistory movieHistory = new MovieHistory();

        movieHistory.clientId = clientId;
        movieHistory.data = data;

        return movieHistory;
    }
}
