package io.knifer.freebox.helper;

import lombok.experimental.UtilityClass;

import java.util.ResourceBundle;

/**
 * 多语言
 *
 * @author Knifer
 * @version 1.0.0
 */
@UtilityClass
public class I18nHelper {

    private final ResourceBundle i18nBundle = ResourceBundle.getBundle("i18n.chs");

    public String getFormatted(String name, Object... args) {
        return String.format(i18nBundle.getString(name), args);
    }

    public String get(String name) {
        return i18nBundle.getString(name);
    }

    public ResourceBundle getBundle() {
        return i18nBundle;
    }
}
