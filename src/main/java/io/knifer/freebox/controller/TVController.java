package io.knifer.freebox.controller;

import io.knifer.freebox.component.converter.SourceBean2StringConverter;
import io.knifer.freebox.component.factory.ClassListCellFactory;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.common.MovieSort;
import io.knifer.freebox.model.common.SourceBean;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.net.websocket.core.KebSocketRunner;
import io.knifer.freebox.net.websocket.core.KebSocketTopicKeeper;
import io.knifer.freebox.net.websocket.template.TVTemplate;
import io.knifer.freebox.net.websocket.template.impl.TVTemplateImpl;
import io.knifer.freebox.util.CastUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;

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

    private TVTemplate template;

    public void destroy() {
        // TODO 销毁时要调用
        WindowHelper.close(root);
    }

    @FXML
    private void initialize() {
        template = new TVTemplateImpl(new KebSocketRunner(KebSocketTopicKeeper.getInstance()));
        Platform.runLater(() -> {
            // TODO converter写入FXML里
            sourceBeanComboBox.setConverter(new SourceBean2StringConverter());
            classesListView.setCellFactory(new ClassListCellFactory());

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
        SourceBean source = sourceBeanComboBox.getSelectionModel().getSelectedItem();

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
            log.info("click item: {}", sortData);
        }
    }

    private ClientInfo getClientInfo() {
        return getData();
    }
}
