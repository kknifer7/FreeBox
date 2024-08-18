package io.knifer.freebox.component.factory;

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

    private final static FontIcon ICON;

    static {
        ICON = new FontIcon("fa-tv:20");
    }

    @Override
    public ListCell<ClientInfo> call(ListView<ClientInfo> param) {

        return new ListCell<>() {
            @Override
            protected void updateItem(ClientInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getConnection().getRemoteSocketAddress().getHostName());
                    setGraphic(ICON);
                }
            }
        };
    }
}
