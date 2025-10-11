package io.knifer.freebox.model.domain;

import io.knifer.freebox.model.c2s.FreeBoxLive;
import lombok.Data;

import java.util.List;

/**
 * 源配置
 *
 * @author Knifer
 */
@Data
public class FreeBoxApiConfig {

    private String url;

    private String spider;

    private List<FreeBoxSourceBean> sites;

    private List<FreeBoxLive> lives;
}
