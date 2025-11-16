package io.knifer.freebox.model.domain;

import javafx.scene.image.Image;
import lombok.Data;

/**
 * 图片加载结果
 *
 * @author Knifer
 */
@Data
public class ImageLoadingResult {

    /**
     * 已加载好的图片
     * 如果加载失败，该字段为默认的占位图片
     */
    private Image image;
    /**
     * 是否加载成功
     */
    private boolean success;

    public static ImageLoadingResult of(Image image, boolean success) {
        ImageLoadingResult result = new ImageLoadingResult();

        result.setImage(image);
        result.setSuccess(success);

        return result;
    }
}
