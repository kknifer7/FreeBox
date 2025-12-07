package io.knifer.freebox.component.factory;

import cn.hutool.core.util.ObjectUtil;
import io.knifer.freebox.constant.ClientType;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ClipboardHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.domain.ClientInfo;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
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
                ContextMenu contextMenu;
                MenuItem copyUrlMenuItem;

                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);

                    return;
                }
                if (getContextMenu() == null) {
                    contextMenu = new ContextMenu();
                    copyUrlMenuItem = new MenuItem(I18nHelper.get(I18nKeys.HOME_CLIENT_CONTEXT_MENU_ITEM_COPY_URL));
                    copyUrlMenuItem.setOnAction(ignored -> {
                        ClipboardHelper.setContent(ObjectUtil.defaultIfNull(item.getConfigUrl(), item::getName));
                        ToastHelper.showInfoI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
                    });
                    contextMenu.getItems().add(copyUrlMenuItem);
                    setContextMenu(contextMenu);
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
