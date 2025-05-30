package io.knifer.freebox.model.common.tvbox;

import lombok.Data;

import java.io.Serializable;

/**
 * @author pj567
 */
@Data
public class AbsXml implements Serializable {

    public Movie movie;

    public String msg;
}