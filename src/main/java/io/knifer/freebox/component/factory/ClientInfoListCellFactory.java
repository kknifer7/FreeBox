package io.knifer.freebox.component.factory;

import io.knifer.freebox.constant.ClientType;
import io.knifer.freebox.model.domain.ClientInfo;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * ClientInfo转String
 *
 * @author Knifer
 */
public class ClientInfoListCellFactory implements Callback<ListView<ClientInfo>, ListCell<ClientInfo>> {

    @Override
    public ListCell<ClientInfo> call(ListView<ClientInfo> param) {

        return new ListCell<>() {
            @Override
            protected void updateItem(ClientInfo item, boolean empty) {
                ClientType clientType;

                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);

                    return;
                }
                setText(item.getName());
                clientType = item.getClientType();
                switch (clientType) {
                    case TVBOX_K:
                        setGraphic(new FontIcon("fa-tablet:20"));
                        break;
                    case CATVOD_SPIDER:
                        setGraphic(new FontIcon("fa-optin-monster:20"));
                        break;
                    case SINGLE_LIVE:
                        setGraphic(new FontIcon("fa-tv:20"));
                        break;
                }
            }
        };
    }
}
