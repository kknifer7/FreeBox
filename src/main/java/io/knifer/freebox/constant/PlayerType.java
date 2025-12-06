package io.knifer.freebox.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PlayerType {

    VLC("VLC", "https://www.videolan.org/vlc/"),
    MPV_EXTERNAL("MPV", "https://mpv.io/installation/");

    private final String name;
    private final String downloadLink;
}
