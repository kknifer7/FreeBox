package io.knifer.freebox.component.validator;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.util.ValidationUtil;
import javafx.scene.control.Control;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.Validator;

/**
 * URL验证器
 *
 * @author Knifer
 */
public class URLValidator implements Validator<String> {

    private final static URLValidator INSTANCE = new URLValidator();

    private URLValidator() {}

    public static URLValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public ValidationResult apply(Control control, String s) {
        String errorMsg;

        if (StringUtils.isEmpty(s) || ValidationUtil.isURL(s)) {
            return new ValidationResult();
        } else {
            errorMsg = I18nHelper.get(I18nKeys.HOME_IMPORT_API_INVALID_URL);

            return ValidationResult.fromError(control, errorMsg);
        }
    }
}
