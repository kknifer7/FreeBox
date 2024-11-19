package io.knifer.freebox.component.factory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.model.common.Movie;
import io.knifer.freebox.model.common.SourceBean;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.ValidationUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.controlsfx.control.InfoOverlay;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 影片单元格工厂
 *
 * @author Knifer
 */
@Slf4j
public class VideoGridCellFactory implements Callback<GridView<Movie.Video>, GridCell<Movie.Video>> {

    private static final Map<String, String> SOURCE_KEY_AND_NAME_MAP = new HashMap<>();

    private static final Cache<String, Image> PICTURE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(150)
            .build();

    private static final Set<String> LOADED_PICTURES = new HashSet<>();

    private final static double CELL_WIDTH = 150;

    private final static double CELL_HEIGHT = 200;

    private final BooleanProperty showSourceName = new SimpleBooleanProperty(false);

    public void setSourceBeans(Collection<SourceBean> sourceBeans) {
        if (CollectionUtil.isEmpty(sourceBeans)) {

            return;
        }
        SOURCE_KEY_AND_NAME_MAP.clear();
        SOURCE_KEY_AND_NAME_MAP.putAll(
                sourceBeans.stream().collect(Collectors.toUnmodifiableMap(
                        SourceBean::getKey, SourceBean::getName
                ))
        );
    }

    public void setShowSourceName(boolean flag) {
        showSourceName.set(flag);
    }

    @Override
    public GridCell<Movie.Video> call(GridView<Movie.Video> view) {
        return new VideoGridCell(showSourceName);
    }

    @RequiredArgsConstructor
    public static class VideoGridCell extends GridCell<Movie.Video> {

        private final BooleanProperty showSourceName;

        @Override
        protected void updateItem(Movie.Video item, boolean empty) {
            String itemId;
            StackPane container;
            List<Node> containerChildren;
            InfoOverlay movieInfoOverlay;
            Label movieNoteLabel;
            Image moviePicImage;
            ImageView moviePicImageView;
            String note;
            String picUrl;
            String sourceName;
            Label sourceNameLabel;
            StackPane sourceNameContainer;

            super.updateItem(item, empty);
            if (item == null || empty) {
                setText(null);
                setGraphic(null);

                return;
            }
            container = new StackPane();
            container.setAlignment(Pos.TOP_RIGHT);
            containerChildren = container.getChildren();
            // 影片左上角源名称
            sourceNameLabel = new Label();
            sourceNameLabel.getStyleClass().add("movie-source-label");
            sourceName = SOURCE_KEY_AND_NAME_MAP.get(item.getSourceKey());
            if (StringUtils.isNotBlank(sourceName)) {
                sourceNameLabel.setText(sourceName);
            }
            sourceNameContainer = new StackPane(sourceNameLabel);
            sourceNameContainer.setAlignment(Pos.TOP_LEFT);
            sourceNameContainer.visibleProperty().bind(showSourceName);
            sourceNameContainer.addEventFilter(MouseEvent.MOUSE_CLICKED, evt -> {
                if (evt.getButton() == MouseButton.PRIMARY) {
                    Event.fireEvent(this, evt);
                }
            });
            // 图片
            itemId = item.getId();
            if (BaseValues.LOAD_MORE_ITEM_ID.equals(itemId)) {
                moviePicImageView = new ImageView(BaseResources.LOAD_MORE_IMG);
            } else {
                moviePicImage = PICTURE_CACHE.getIfPresent(itemId);
                if (moviePicImage == null) {
                    moviePicImageView = new ImageView(BaseResources.PICTURE_PLACEHOLDER_IMG);
                    picUrl = item.getPic();
                    if (!LOADED_PICTURES.contains(picUrl) && ValidationUtil.isURL(picUrl)) {
                        LOADED_PICTURES.add(picUrl);
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
            // 影片右上角备注
            note = item.getNote();
            containerChildren.add(movieInfoOverlay);
            containerChildren.add(sourceNameContainer);
            if (StringUtils.isNotBlank(note)) {
                movieNoteLabel = new Label(note);
                movieNoteLabel.getStyleClass().add("movie-remark-label");
                containerChildren.add(movieNoteLabel);
            }
            setId(item.getId());
            setGraphic(container);
        }
    }

    public void destroy() {
        PICTURE_CACHE.invalidateAll();
    }
}
