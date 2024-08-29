package io.knifer.freebox.controller;

import io.knifer.freebox.component.node.VLCPlayer;
import io.knifer.freebox.component.converter.SourceBean2StringConverter;
import io.knifer.freebox.component.factory.ClassListCellFactory;
import io.knifer.freebox.component.factory.VideoGridCellFactory;
import io.knifer.freebox.constant.Views;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.bo.VideoDetailsBO;
import io.knifer.freebox.model.common.Movie;
import io.knifer.freebox.model.common.MovieSort;
import io.knifer.freebox.model.common.SourceBean;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.s2c.GetCategoryContentDTO;
import io.knifer.freebox.model.s2c.GetDetailContentDTO;
import io.knifer.freebox.net.websocket.core.KebSocketRunner;
import io.knifer.freebox.net.websocket.core.KebSocketTopicKeeper;
import io.knifer.freebox.net.websocket.template.TVTemplate;
import io.knifer.freebox.net.websocket.template.impl.TVTemplateImpl;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.FXMLUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.GridView;

import java.util.List;

/**
 * 客户端电视
 *
 * @author Knifer
 */
@Slf4j
public class TVController extends BaseController {

    @FXML
    private BorderPane root;
    @FXML
    private ComboBox<SourceBean> sourceBeanComboBox;
    @FXML
    private ListView<MovieSort.SortData> classesListView;
    @FXML
    private GridView<Movie.Video> videosGridView;

    private TVTemplate template;

    public void destroy() {
        log.info(
                "[{}]'s tv controller destroy",
                getClientInfo().getConnection().getRemoteSocketAddress().getHostName()
        );
        if (videosGridView.getCellFactory() instanceof VideoGridCellFactory factory) {
            factory.destroy();
        }
    }

    @FXML
    private void initialize() {
        template = new TVTemplateImpl(new KebSocketRunner(KebSocketTopicKeeper.getInstance()));
        Platform.runLater(() -> {
           WindowHelper.getStage(root).setOnCloseRequest(evt -> {
               destroy();
               Context.INSTANCE.popAndShowLastStage();
           });

            // TODO converter写入FXML里
            sourceBeanComboBox.setConverter(new SourceBean2StringConverter());
            classesListView.setCellFactory(new ClassListCellFactory());
            videosGridView.setCellFactory(new VideoGridCellFactory());

            videosGridView.setOnMouseClicked(evt -> {
                Movie.Video video;

                if (evt.getTarget() instanceof VideoGridCellFactory.VideoGridCell cell) {
                    if (evt.getClickCount() > 1) {
                        video = cell.getItem();
                        template.getDetailContent(
                                getClientInfo(),
                                GetDetailContentDTO.of(getCurrentSourceBean().getKey(), video.getId()),
                                detailContent -> {
                                    Pair<Stage, VideoController> stageAndController;
                                    Stage tvStage;
                                    Stage videoStage;

                                    if (
                                            detailContent.getMovie()
                                                    .getVideoList()
                                                    .stream()
                                                    .noneMatch(v -> StringUtils.isNotBlank(v.getId()))
                                    ) {
                                        // 有的源接口会在影视列表数据中插入广告，因此需要通过影视ID来过滤掉无效的影视
                                        // 多次点击这样的无效影视会导致客户端断开链接，这是TVBox-K的问题，待解决
                                        return;
                                    }
                                    stageAndController = FXMLUtil.load(Views.VIDEO);
                                    tvStage = WindowHelper.getStage(root);
                                    videoStage = stageAndController.getLeft();
                                    videoStage.setTitle(video.getName());
                                    stageAndController.getRight().setData(VideoDetailsBO.of(
                                            detailContent,
                                            new VLCPlayer((BorderPane) videoStage.getScene().getRoot())
                                    ));
                                    Context.INSTANCE.pushStage(tvStage);
                                    tvStage.hide();
                                    videoStage.show();
                                }
                        );
                    }
                }
            });
            template.getSourceBeanList(getClientInfo(), this::initSourceBeanData);
        });
    }

    private void initSourceBeanData(List<SourceBean> sourceBeanList) {
        List<SourceBean> items = sourceBeanComboBox.getItems();

        items.clear();
        if (sourceBeanList.isEmpty()) {
            return;
        }
        items.addAll(sourceBeanList);
    }

    @FXML
    private void onSourceBeanComboBoxAction() {
        SourceBean source = getCurrentSourceBean();

        template.getHomeContent(getClientInfo(), source, homeContent -> {
            MovieSort classes = homeContent.getClasses();
            List<MovieSort.SortData> sortList = classes.getSortList();
            ObservableList<MovieSort.SortData> items = classesListView.getItems();

            items.clear();
            items.addAll(sortList);
        });
    }

    @FXML
    private void onClassesListViewClick(MouseEvent mouseEvent) {
        MovieSort.SortData sortData;

        if (mouseEvent.getTarget() instanceof ListCell<?> listCell) {
            sortData = CastUtil.cast(listCell.getItem());
            if (sortData == null) {
                return;
            }
            template.getCategoryContent(
                    getClientInfo(),
                    GetCategoryContentDTO.of(getCurrentSourceBean(), sortData, 1),
                    categoryContent -> {
                        ObservableList<Movie.Video> items = videosGridView.getItems();
                        List<Movie.Video> videos = categoryContent.getMovie().getVideoList();

                        if (!items.isEmpty()) {
                            /*
                            * 列表中原本有影片数据，在清空的同时，也要清空异步任务队列
                            * 防止列表中旧有影片封面的异步加载任务占用异步线程
                            */
                            items.clear();
                            AsyncUtil.cancelAllTask();
                        }
                        if (!videos.isEmpty()) {
                            items.addAll(videos);
                        }
                    }
            );
        }
    }

    private SourceBean getCurrentSourceBean() {
        return sourceBeanComboBox.getSelectionModel().getSelectedItem();
    }

    private ClientInfo getClientInfo() {
        return getData();
    }
}
