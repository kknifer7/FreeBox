package io.knifer.freebox.component.validator;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.util.ValidationUtil;
import javafx.scene.control.Control;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.Validator;

/**
 * 端口验证器
 *
 * @author Knifer
 */
public class PortValidator implements Validator<String> {

    private final static PortValidator INSTANCE = new PortValidator();

    private PortValidator() {}

    public static PortValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public ValidationResult apply(Control control, String s) {
        String errorMsg;

        if (ValidationUtil.isPort(s)) {
            return new ValidationResult();
        } else {
            errorMsg = I18nHelper.get(I18nKeys.SETTINGS_INVALID_PORT_MESSAGE);

            return ValidationResult.fromError(control, errorMsg);
        }

    }
}
