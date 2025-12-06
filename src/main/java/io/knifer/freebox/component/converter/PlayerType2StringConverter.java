package io.knifer.freebox.component.converter;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.PlayerType;
import io.knifer.freebox.helper.I18nHelper;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;

/**
 * 播放器类型 数据转换器
 *
 * @author Knifer
 */
public class PlayerType2StringConverter extends StringConverter<PlayerType> {

    @Override
    public String toString(PlayerType playerType) {
        return playerType == null ? StringUtils.EMPTY : switch (playerType) {
            case VLC -> I18nHelper.get(I18nKeys.SETTINGS_PLAYER_VLC);
            case MPV_EXTERNAL -> I18nHelper.get(I18nKeys.SETTINGS_PLAYER_MPV_EXTERNAL);
            default -> throw new IllegalArgumentException("Unexpected value: " + playerType);
        };
    }

    @Override
    public PlayerType fromString(String s) {
        return PlayerType.valueOf(s);
    }
}
