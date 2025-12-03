package io.knifer.freebox.model.domain;

import io.github.filelize.Filelize;
import io.github.filelize.FilelizeType;
import io.github.filelize.Id;
import lombok.Data;

/**
 * 客户端TV相关属性
 *
 * @author Knifer
 */
@Data
@Filelize(name = "client_tv_properties", type = FilelizeType.MULTIPLE_FILES, directory = "client_tv_properties")
public class ClientTVProperties implements Savable, Copyable<ClientTVProperties> {

    /**
     * 客户端ID
     */
    @Id
    private String id;

    /**
     * 最后使用的站点key
     */
    private String sourceKeyLastUsed;

    public static ClientTVProperties from(String id) {
        ClientTVProperties result = new ClientTVProperties();

        result.id = id;

        return result;
    }

    @Override
    public ClientTVProperties copy() {
        ClientTVProperties result = new ClientTVProperties();

        result.id = id;
        result.sourceKeyLastUsed = sourceKeyLastUsed;

        return result;
    }
}
