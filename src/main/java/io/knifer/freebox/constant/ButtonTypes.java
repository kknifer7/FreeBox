package io.knifer.freebox.constant;

import io.knifer.freebox.helper.I18nHelper;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.dialog.CommandLinksDialog;

/**
 * FreeBox按钮类型
 *
 * @author Knifer
 */
@UtilityClass
public class ButtonTypes {

    public static final ButtonType OK =
            new ButtonType(I18nHelper.get(I18nKeys.COMMON_OK), ButtonBar.ButtonData.OK_DONE);

    public static final ButtonType OPEN_DIRECTLY =
            new ButtonType(I18nHelper.get(I18nKeys.UPGRADE_INSTALL_DIALOG_OPEN_DIRECTLY), ButtonBar.ButtonData.OK_DONE);

    public static final ButtonType OPEN_PATH =
            new ButtonType(I18nHelper.get(I18nKeys.UPGRADE_INSTALL_DIALOG_OPEN_PATH), ButtonBar.ButtonData.OK_DONE);

    public static final CommandLinksDialog.CommandLinksButtonType CAT_VOD_COMMAND_LINKS_BUTTON_TYPE = new CommandLinksDialog.CommandLinksButtonType(
            I18nHelper.get(I18nKeys.HOME_IMPORT_API_SELECT_TYPE_CAT_VOD_TYPE_TITLE),
            I18nHelper.get(I18nKeys.HOME_IMPORT_API_SELECT_TYPE_CAT_VOD_TYPE_CONTENT),
            true
    );
    public static final CommandLinksDialog.CommandLinksButtonType SINGLE_LIVE_COMMAND_LINKS_BUTTON_TYPE = new CommandLinksDialog.CommandLinksButtonType(
            I18nHelper.get(I18nKeys.HOME_IMPORT_API_SELECT_TYPE_SINGLE_LIVE_TYPE_TITLE),
            I18nHelper.get(I18nKeys.HOME_IMPORT_API_SELECT_TYPE_SINGLE_LIVE_TYPE_CONTENT),
            false
    );
    public static final CommandLinksDialog.CommandLinksButtonType CANCEL_COMMAND_LINKS_BUTTON_TYPE = new CommandLinksDialog.CommandLinksButtonType(
            I18nHelper.get(I18nKeys.COMMON_CANCEL),
            StringUtils.EMPTY,
            false
    );
}
