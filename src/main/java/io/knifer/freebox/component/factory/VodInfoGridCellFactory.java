package io.knifer.freebox.component.factory;

import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.VideoPlaybackTrigger;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ImageHelper;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.model.common.tvbox.VodInfo;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 影片信息单元格工厂（历史记录、收藏夹）
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor
public class VodInfoGridCellFactory implements Callback<GridView<VodInfo>, GridCell<VodInfo>> {

    private final static Map<String, StackPane> ITEM_ID_AND_CONTAINER = new HashMap<>();

    private final static double CELL_WIDTH = 150;

    private final static double CELL_HEIGHT = 200;

    private final Map<String, String> sourceKeyAndNameMap = new HashMap<>();
    private final Consumer<VodInfo> onItemAction;
    private final Consumer<VodInfo> onItemDelete;

    public void setSourceBeans(Collection<SourceBean> sourceBeans) {
        sourceKeyAndNameMap.clear();
        sourceKeyAndNameMap.putAll(
                sourceBeans.stream().collect(Collectors.toUnmodifiableMap(
                        SourceBean::getKey, SourceBean::getName, (v1, v2) -> v2
                ))
        );
    }

    @Override
    public GridCell<VodInfo> call(GridView<VodInfo> param) {
        VodInfoGridCell cell = new VodInfoGridCell(sourceKeyAndNameMap, onItemAction, onItemDelete);

        cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        return cell;
    }

    @RequiredArgsConstructor
    public static class VodInfoGridCell extends GridCell<VodInfo> {

        private EventHandler<MouseEvent> actionEventFilter;
        private final Map<String, String> sourceKeyAndNameMap;
        private final Consumer<VodInfo> onItemAction;
        private final Consumer<VodInfo> onItemDelete;

        @Override
        protected void updateItem(VodInfo item, boolean empty) {
            StackPane container;
            String itemId;
            List<Node> containerChildren;
            InfoOverlay movieInfoOverlay;
            Label movieNoteLabel;
            ImageView moviePicImageView;
            String note;
            String picUrl;
            String sourceName;
            Label sourceNameLabel;
            StackPane sourceNameContainer;

            super.updateItem(item, empty);
            if (item == null || empty) {
                setGraphic(null);

                return;
            }
            itemId = item.getId();
            container = ITEM_ID_AND_CONTAINER.get(itemId);
            if (container == null) {
                container = new StackPane();
                container.setAlignment(Pos.TOP_RIGHT);
                containerChildren = container.getChildren();
                // 影片左上角源名称
                sourceNameLabel = new Label();
                sourceNameLabel.getStyleClass().add("movie-source-label");
                sourceName = sourceKeyAndNameMap.get(item.getSourceKey());
                if (StringUtils.isNotBlank(sourceName)) {
                    sourceNameLabel.setText(sourceName);
                }
                sourceNameContainer = new StackPane(sourceNameLabel);
                sourceNameContainer.setAlignment(Pos.TOP_LEFT);
                // 图片
                moviePicImageView = new ImageView();
                if (BaseValues.LOAD_MORE_ITEM_ID.equals(itemId)) {
                    moviePicImageView.setImage(BaseResources.LOAD_MORE_IMG);
                } else {
                    moviePicImageView.setImage(BaseResources.PICTURE_PLACEHOLDER_IMG);
                    if (itemId != null) {
                        picUrl = item.getPic();
                        ImageHelper.loadAsync(picUrl)
                                .thenAccept(result -> {
                                    if (result.isSuccess()) {
                                        Platform.runLater(() -> moviePicImageView.setImage(result.getImage()));
                                    }
                                });
                    }
                }
                moviePicImageView.setFitWidth(CELL_WIDTH);
                moviePicImageView.setFitHeight(CELL_HEIGHT);
                movieInfoOverlay = new InfoOverlay(moviePicImageView, item.getName());
                sourceNameContainer.addEventFilter(MouseEvent.MOUSE_CLICKED, evt -> {
                    if (Objects.requireNonNull(evt.getButton()) == MouseButton.SECONDARY) {
                        movieInfoOverlay.getContextMenu()
                                .show(movieInfoOverlay, evt.getScreenX(), evt.getScreenY());
                    }
                });
                containerChildren.add(movieInfoOverlay);
                containerChildren.add(sourceNameContainer);
                // 影片右上角备注
                note = item.getNote();
                if (StringUtils.isNotBlank(note)) {
                    movieNoteLabel = new Label(note);
                    movieNoteLabel.getStyleClass().add("movie-remark-label");
                    containerChildren.add(movieNoteLabel);
                }
                ITEM_ID_AND_CONTAINER.put(itemId, container);
            } else {
                containerChildren = container.getChildren();
                movieInfoOverlay = (InfoOverlay) containerChildren.get(0);
            }
            setupActionEventFilter(item);
            setupMovieInfoOverlay(movieInfoOverlay, item);
            if (ConfigHelper.getVideoPlaybackTrigger() == VideoPlaybackTrigger.SINGLE_CLICK) {
                setCursor(Cursor.HAND);
            } else if (getCursor() != Cursor.DEFAULT) {
                setCursor(Cursor.DEFAULT);
            }
            setGraphic(container);
            setId(itemId);
        }

        private void setupActionEventFilter(VodInfo vod) {
            if (actionEventFilter != null) {
                removeEventFilter(MouseEvent.MOUSE_CLICKED, actionEventFilter);
            }
            actionEventFilter = event -> {
                VideoPlaybackTrigger playbackTrigger;
                int expectClickCount;

                if (event.getButton() != MouseButton.PRIMARY) {

                    return;
                }
                playbackTrigger = ConfigHelper.getVideoPlaybackTrigger();
                expectClickCount = playbackTrigger == VideoPlaybackTrigger.SINGLE_CLICK ? 1 : 2;
                if (event.getClickCount() != expectClickCount) {

                    return;
                }
                onItemAction.accept(vod);
            };
            addEventFilter(MouseEvent.MOUSE_CLICKED, actionEventFilter);
        }

        private void setupMovieInfoOverlay(InfoOverlay infoOverlay, VodInfo vodInfo) {
            ContextMenu contextMenu = infoOverlay.getContextMenu();
            MenuItem deleteMenuItem;

            if (contextMenu == null) {
                infoOverlay.getStyleClass().add("movie-info-overlay");
                contextMenu = new ContextMenu();
                contextMenu.setUserData(vodInfo);
                infoOverlay.setContextMenu(contextMenu);
                deleteMenuItem = new MenuItem(I18nHelper.get(I18nKeys.COMMON_DELETE));
                deleteMenuItem.setOnAction(evt -> onItemDelete.accept(vodInfo));
                contextMenu.getItems().add(deleteMenuItem);
            } else {
                deleteMenuItem = contextMenu.getItems().get(0);
                if (contextMenu.getUserData() != vodInfo) {
                    contextMenu.setUserData(vodInfo);
                    deleteMenuItem.setOnAction(evt -> onItemDelete.accept(vodInfo));
                }
            }
        }
    }

    public void destroy() {
        ITEM_ID_AND_CONTAINER.clear();
    }
}
