package io.knifer.freebox.constant;

import com.google.gson.annotations.SerializedName;

public enum Platform {

    @SerializedName("windows")
    WINDOWS,

    @SerializedName("mac")
    MAC,

    @SerializedName("deb_linux")
    DEB_LINUX,

    @SerializedName("rpm_linux")
    RPM_LINUX,

    @SerializedName("other_linux")
    OTHER_LINUX,
}
