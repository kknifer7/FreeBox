package io.knifer.freebox.component.factory;

import io.knifer.freebox.component.node.EmojiableLabel;
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
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.controlsfx.control.InfoOverlay;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final boolean historyFlag;
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
        VodInfoGridCell cell = new VodInfoGridCell(historyFlag, sourceKeyAndNameMap, onItemAction, onItemDelete);

        cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        return cell;
    }

    @RequiredArgsConstructor
    public static class VodInfoGridCell extends GridCell<VodInfo> {

        private EventHandler<MouseEvent> actionEventFilter;
        private final boolean historyFlag;
        private final Map<String, String> sourceKeyAndNameMap;
        private final Consumer<VodInfo> onItemAction;
        private final Consumer<VodInfo> onItemDelete;

        @Override
        protected void updateItem(VodInfo item, boolean empty) {
            StackPane container;
            String itemId;
            List<Node> containerChildren;
            InfoOverlay movieInfoOverlay;
            Label movieRemarkLabel;
            ImageView moviePicImageView;
            String picUrl;
            String sourceName;
            EmojiableLabel sourceNameLabel;
            AnchorPane tagContainer;
            ProgressIndicator progressIndicator;
            StackPane progressContainer;

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
                sourceNameLabel = new EmojiableLabel();
                sourceNameLabel.getStyleClass().add("movie-source-label");
                sourceNameLabel.setMaxWidth(CELL_WIDTH / 2);
                sourceName = sourceKeyAndNameMap.get(item.getSourceKey());
                if (StringUtils.isNotBlank(sourceName)) {
                    sourceNameLabel.setText(sourceName);
                }
                AnchorPane.setTopAnchor(sourceNameLabel, 0d);
                AnchorPane.setLeftAnchor(sourceNameLabel, 0d);
                // 影片右上角备注（需要根据历史记录、收藏夹的不同性质动态更新，因此先不设置文本）
                movieRemarkLabel = new Label();
                movieRemarkLabel.getStyleClass().add("movie-remark-label");
                movieRemarkLabel.setMaxWidth(CELL_WIDTH / 2);
                AnchorPane.setTopAnchor(movieRemarkLabel, 0d);
                AnchorPane.setRightAnchor(movieRemarkLabel, 0d);
                tagContainer = new AnchorPane(sourceNameLabel, movieRemarkLabel);
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
                // 播放进度提示
                progressIndicator = new ProgressIndicator();
                progressIndicator.getStyleClass().add("movie-progress-indicator");
                progressContainer = new StackPane(progressIndicator);
                // 添加结点到容器
                containerChildren.add(movieInfoOverlay);
                containerChildren.add(tagContainer);
                containerChildren.add(progressContainer);
                ITEM_ID_AND_CONTAINER.put(itemId, container);
            } else {
                containerChildren = container.getChildren();
                movieInfoOverlay = (InfoOverlay) containerChildren.get(0);
                tagContainer = (AnchorPane) containerChildren.get(1);
                progressContainer = (StackPane) containerChildren.get(2);
                movieRemarkLabel = (Label) tagContainer.getChildren().get(1);
            }
            setupMovieInfoOverlay(movieInfoOverlay, item);
            setupRemarkAndProgress(movieRemarkLabel, progressContainer, item);
            setupActionEventFilter(movieInfoOverlay, item);
            setupCursor();
            setGraphic(container);
            setId(itemId);
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

        private void setupRemarkAndProgress(
                Label movieRemarkLabel,
                StackPane progressContainer,
                VodInfo vodInfo
        ) {
            String remark;
            double progressVal;
            ProgressIndicator progressIndicator;

            if (historyFlag) {
                remark = vodInfo.getPlayNote();
                if ((progressVal = calculateProgress(vodInfo)) > 0) {
                    progressIndicator = (ProgressIndicator) progressContainer.getChildren().get(0);
                    progressIndicator.setProgress(progressVal);
                    progressContainer.setVisible(true);
                } else {
                    progressContainer.setVisible(false);
                }
            } else {
                remark = vodInfo.getNote();
                progressContainer.setVisible(false);
            }
            if (StringUtils.isBlank(remark)) {
                movieRemarkLabel.setVisible(false);
            } else {
                movieRemarkLabel.setText(remark);
                movieRemarkLabel.setVisible(true);
            }
        }

        private double calculateProgress(VodInfo vodInfo) {
            Long progress = vodInfo.getProgress();
            Long duration = vodInfo.getDuration();

            return (progress != null && duration != null && progress >= 0 && duration > 0 && progress <= duration) ?
                    (double) progress / duration : 0;
        }

        private void setupActionEventFilter(InfoOverlay movieInfoOverlay, VodInfo vod) {
            if (actionEventFilter != null) {
                removeEventFilter(MouseEvent.MOUSE_CLICKED, actionEventFilter);
            }
            actionEventFilter = event -> {
                MouseButton button = event.getButton();
                VideoPlaybackTrigger playbackTrigger;
                int expectClickCount;

                if (button == null) {
                    return;
                }
                switch (button) {
                    case PRIMARY:
                        // 影片打开Action
                        playbackTrigger = ConfigHelper.getVideoPlaybackTrigger();
                        expectClickCount = playbackTrigger == VideoPlaybackTrigger.SINGLE_CLICK ? 1 : 2;
                        if (event.getClickCount() == expectClickCount) {
                            onItemAction.accept(vod);
                        }
                        break;
                    case SECONDARY:
                        // 右键菜单
                        movieInfoOverlay.getContextMenu()
                                .show(movieInfoOverlay, event.getScreenX(), event.getScreenY());
                        break;
                }
            };
            addEventFilter(MouseEvent.MOUSE_CLICKED, actionEventFilter);
        }

        private void setupCursor() {
            Cursor cursor = ConfigHelper.getVideoPlaybackTrigger() == VideoPlaybackTrigger.SINGLE_CLICK ?
                    Cursor.HAND : Cursor.DEFAULT;

            if (cursor != getCursor()) {
                setCursor(cursor);
            }
        }
    }

    public void destroy() {
        ITEM_ID_AND_CONTAINER.clear();
    }
}
