package io.knifer.freebox.constant;

import io.knifer.freebox.helper.I18nHelper;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import lombok.experimental.UtilityClass;

/**
 * FreeBox按钮类型
 *
 * @author Knifer
 */
@UtilityClass
public class ButtonTypes {

    public static final ButtonType EXIT =
            new ButtonType(I18nHelper.get(I18nKeys.COMMON_EXIT), ButtonBar.ButtonData.CANCEL_CLOSE);
}
