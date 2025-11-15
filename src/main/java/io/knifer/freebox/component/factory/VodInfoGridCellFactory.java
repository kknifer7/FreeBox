package io.knifer.freebox.component.factory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.HttpHeaders;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.model.common.tvbox.VodInfo;
import io.knifer.freebox.util.ValidationUtil;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.controlsfx.control.InfoOverlay;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

    private final static Cache<String, Image> PICTURE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build();
    private final static Set<String> LOADED_PICTURES = new HashSet<>();
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
            Image moviePicImage;
            Image newMoviePicImage;
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
                    if (itemId == null) {
                        moviePicImageView.setImage(BaseResources.PICTURE_PLACEHOLDER_IMG);
                    } else if ((moviePicImage = PICTURE_CACHE.getIfPresent(itemId)) == null) {
                        moviePicImageView.setImage(BaseResources.PICTURE_PLACEHOLDER_IMG);
                        picUrl = item.getPic();
                        if (!LOADED_PICTURES.contains(picUrl) && ValidationUtil.isURL(picUrl)) {
                            /*LOADED_PICTURES.add(picUrl);
                            newMoviePicImage = new Image(picUrl, true);
                            newMoviePicImage.progressProperty().addListener(observable -> {
                                if (newMoviePicImage.getProgress() >= 1.0 && !newMoviePicImage.isError()) {
                                    moviePicImageView.setImage(newMoviePicImage);
                                    PICTURE_CACHE.put(itemId, newMoviePicImage);
                                }
                                else if (newMoviePicImage.getProgress() >= 1.0 && newMoviePicImage.isError()) {
                                    moviePicImageView.setImage(BaseResources.PICTURE_PLACEHOLDER_IMG);
                                    loadCustomImage(picUrl, moviePicImageView, itemId);
                                }
                            });*/
                            moviePicImageView.setImage(BaseResources.PICTURE_PLACEHOLDER_IMG);
                            loadCustomImage(picUrl, moviePicImageView, itemId);
                        }
                    } else {
                        moviePicImageView.setImage(moviePicImage);
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
            setGraphic(container);
            setId(itemId);
        }

        private void setupActionEventFilter(VodInfo vod) {
            if (actionEventFilter != null) {
                removeEventFilter(MouseEvent.MOUSE_CLICKED, actionEventFilter);
            }
            actionEventFilter = event -> {
                if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() < 2) {

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

    // 使用 HttpClient 替代 Image 直接加载
    public static void loadCustomImage(String picUrl, ImageView imageView, String itemId) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(picUrl).addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36").build();

        client.newCall(request).enqueue(new okhttp3.Callback() {


            @Override
            public void onFailure(Call call, IOException e) {

                Platform.runLater(() -> {
                    imageView.setImage(BaseResources.PICTURE_PLACEHOLDER_IMG);
                });
                log.error("Error download image: {}", picUrl, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    byte[] imageData = response.body().bytes();
                    Image image = new Image(new ByteArrayInputStream(imageData));
                    if (image.getProgress() >= 1.0 && !image.isError()) {
                        Platform.runLater(() -> {
                            imageView.setImage(image);
                            PICTURE_CACHE.put(itemId, image);
                            LOADED_PICTURES.add(picUrl);
                        });
                    } else if (image.isError()) {

                        //使用代理，增加兼容性
                        loadCustomImage("https://cdn.cdnjson.com/pic.html?url=" + picUrl, imageView, itemId);
                    } else {
                        log.error(" 加载 image error : {} ", picUrl + image.getException().getMessage());
                    }
                }
            }
        });

    }

    public void destroy() {
        PICTURE_CACHE.invalidateAll();
        LOADED_PICTURES.clear();
        ITEM_ID_AND_CONTAINER.clear();
    }
}
