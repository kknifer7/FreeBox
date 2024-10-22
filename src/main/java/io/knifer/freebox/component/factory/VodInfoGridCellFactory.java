package io.knifer.freebox.component.factory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.model.common.VodInfo;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.ValidationUtil;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.controlsfx.control.InfoOverlay;

import java.util.List;

/**
 * 影片信息单元格工厂
 *
 * @author Knifer
 */
public class VodInfoGridCellFactory implements Callback<GridView<VodInfo>, GridCell<VodInfo>> {

    private static final Cache<String, Image> PICTURE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build();

    private final static double CELL_WIDTH = 150;

    private final static double CELL_HEIGHT = 200;

    @Override
    public GridCell<VodInfo> call(GridView<VodInfo> param) {
        return new VodInfoGridCell();
    }

    public static class VodInfoGridCell extends GridCell<VodInfo> {
        @Override
        protected void updateItem(VodInfo item, boolean empty) {
            String itemId;
            StackPane container;
            List<Node> containerChildren;
            InfoOverlay movieInfoOverlay;
            Label movieNoteLabel;
            Image moviePicImage;
            ImageView moviePicImageView;
            String note;
            String picUrl;

            super.updateItem(item, empty);
            if (item == null || empty) {
                setText(null);
                setGraphic(null);

                return;
            }
            container = new StackPane();
            container.setAlignment(Pos.TOP_RIGHT);
            containerChildren = container.getChildren();
            itemId = item.getId();
            if (BaseValues.LOAD_MORE_ITEM_ID.equals(itemId)) {
                moviePicImageView = new ImageView(BaseResources.LOAD_MORE_IMG);
            } else {
                moviePicImage = PICTURE_CACHE.getIfPresent(itemId);
                if (moviePicImage == null) {
                    moviePicImageView = new ImageView(BaseResources.PICTURE_PLACEHOLDER_IMG);
                    picUrl = item.getPic();
                    if (ValidationUtil.isURL(picUrl)) {
                        AsyncUtil.execute(
                                () -> new Image(picUrl),
                                image -> {
                                    if (!image.isError()) {
                                        Platform.runLater(() -> {
                                            PICTURE_CACHE.put(itemId, image);
                                            updateItem(item, false);
                                        });
                                    }
                                }
                        );
                    }
                } else {
                    moviePicImageView = new ImageView(moviePicImage);
                }
            }
            moviePicImageView.setFitWidth(CELL_WIDTH);
            moviePicImageView.setFitHeight(CELL_HEIGHT);
            movieInfoOverlay = new InfoOverlay(moviePicImageView, item.getName());
            movieInfoOverlay.getStyleClass().add("movie-info-overlay");
            note = item.getNote();
            containerChildren.add(movieInfoOverlay);
            if (StringUtils.isNotBlank(note)) {
                movieNoteLabel = new Label(note);
                movieNoteLabel.getStyleClass().add("movie-remark-label");
                containerChildren.add(movieNoteLabel);
            }
            setId(item.getId());
            setGraphic(container);
        }
    }
}