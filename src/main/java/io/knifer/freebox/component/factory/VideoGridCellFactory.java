package io.knifer.freebox.component.factory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.model.common.Movie;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.controlsfx.control.InfoOverlay;

import java.util.List;

/**
 * 影片单元格工厂
 *
 * @author Knifer
 */
@Slf4j
public class VideoGridCellFactory implements Callback<GridView<Movie.Video>, GridCell<Movie.Video>> {

    private final Cache<String, Image> PICTURE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(150)
            .build();

    @Override
    public GridCell<Movie.Video> call(GridView<Movie.Video> view) {
        return new GridCell<>() {
            @Override
            protected void updateItem(Movie.Video item, boolean empty) {
                String itemId;
                StackPane container;
                List<Node> containerChildren;
                Label movieNoteLabel;
                InfoOverlay movieInfoOverlay;
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
                itemId = item.getId();
                moviePicImage = PICTURE_CACHE.getIfPresent(itemId);
                if (moviePicImage == null) {
                    moviePicImageView = new ImageView(BaseResources.PICTURE_PLACEHOLDER);
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
                moviePicImageView.setFitWidth(150);
                moviePicImageView.setFitHeight(200);
                movieInfoOverlay = new InfoOverlay(moviePicImageView, item.getName());
                movieInfoOverlay.getStyleClass().add("movie-info-overlay");
                container = new StackPane();
                containerChildren = container.getChildren();
                containerChildren.add(movieInfoOverlay);
                note = item.getNote();
                if (StringUtils.isNotBlank(note)) {
                    movieNoteLabel = new Label(note);
                    containerChildren.add(movieNoteLabel);
                    movieNoteLabel.getStyleClass().add("movie-remark-label");
                    container.setAlignment(Pos.TOP_RIGHT);
                }
                setGraphic(container);
            }
        };
    }

    public void destroy() {
        PICTURE_CACHE.invalidateAll();
    }
}
