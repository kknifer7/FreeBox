package io.knifer.freebox.model.common.tvbox;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author pj567
 */
@Data
public class AbsSortXml implements Serializable {

    public MovieSort classes;

    public Movie list;

    public List<Movie.Video> videoList;
}