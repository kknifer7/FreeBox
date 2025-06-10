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
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);

                    return;
                }
                setText(item.getName());
                if (item.getClientType() == ClientType.TVBOX_K) {
                    setGraphic(new FontIcon("fa-tv:20"));
                } else {
                    setGraphic(new FontIcon("fa-optin-monster:20"));
                }
            }
        };
    }
}
