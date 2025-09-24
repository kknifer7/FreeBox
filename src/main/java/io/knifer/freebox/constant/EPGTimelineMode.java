package io.knifer.freebox.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;
import java.time.LocalTime;

/**
 * EPG界面 - 时间线模式
 *
 * @author Knifer
 */
@Getter
@AllArgsConstructor
public enum EPGTimelineMode {

    // 正点（默认）
    COMMON("0", LocalTime.of(6, 0), LocalTime.of(23, 0)),
    // 上午
    MORNING("1", LocalTime.of(0, 0), LocalTime.of(12, 0)),
    // 下午
    AFTERNOON("2", LocalTime.of(12, 0), LocalTime.of(23, 59)),
    // 全天
    WHOLE_DAY("3", LocalTime.of(0, 0), LocalTime.of(23, 59));

    private final String value;
    private final LocalTime startTime;
    private final LocalTime endTime;

    @Nullable
    public static EPGTimelineMode getByValue(String value) {
        for (EPGTimelineMode mode : values()) {
            if (mode.value.equals(value)) {

                return mode;
            }
        }

        return null;
    }
}
