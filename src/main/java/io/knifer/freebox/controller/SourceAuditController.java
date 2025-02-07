package io.knifer.freebox.controller;

import io.knifer.freebox.component.factory.SourceBeanListCellFactory;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.SourceAuditResult;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.ClipboardHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.bo.SourceAuditExecutionBo;
import io.knifer.freebox.model.common.SourceBean;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.domain.SourceAuditItem;
import io.knifer.freebox.net.websocket.template.KebSocketTemplate;
import io.knifer.freebox.net.websocket.template.impl.KebSocketTemplateImpl;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditExecutor;
import io.knifer.freebox.service.sourceaudit.auditor.impl.SourceAuditExecutorImpl;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.IndexedCheckModel;
import org.controlsfx.control.tableview2.TableColumn2;
import org.controlsfx.control.tableview2.TableView2;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 源审计
 *
 * @author Knifer
 */
@Slf4j
public class SourceAuditController extends BaseController{

    @FXML
    private BorderPane root;
    @FXML
    private CheckListView<SourceBean> sourceBeanCheckListView;
    @FXML
    private TableView2<SourceAuditItem> sourceAuditItemTableView;
    @FXML
    private TableColumn2<SourceAuditItem, String> sourceAuditItemNameTableColumn;
    @FXML
    private TableColumn2<SourceAuditItem, LocalDateTime> sourceAuditItemStartAtTableColumn;
    @FXML
    private TableColumn2<SourceAuditItem, Double> sourceAuditItemCostTableColumn;
    @FXML
    private TableColumn2<SourceAuditItem, SourceAuditStatus> sourceAuditItemStatusTableColumn;
    @FXML
    private TableColumn2<SourceAuditItem, String> sourceAuditItemResultInfoTableColumn;
    @FXML
    private TextArea requestRawDataTextArea;
    @FXML
    private TextArea responseRawDataTextArea;
    @FXML
    private Button singleSourceStartAuditBtn;
    @FXML
    private Button startAuditBtn;

    private KebSocketTemplate template;
    private ClientInfo clientInfo;

    private final SourceAuditExecutor sourceAuditExecutor = SourceAuditExecutorImpl.getInstance();
    private final SimpleObjectProperty<SourceBean> nowSourceBeanProperty = new SimpleObjectProperty<>();
    private final BooleanProperty loadingProperty = new SimpleBooleanProperty(true);
    private final Map<String, ObservableList<SourceAuditItem>> sourceKeyAndAuditItemsMap = new HashMap<>();

    @FXML
    private void initialize() {
        template = KebSocketTemplateImpl.getInstance();

        sourceBeanCheckListView.setCellFactory(new SourceBeanListCellFactory(
                sourceBeanCheckListView,
                sourceBean -> {
                    ObservableList<SourceAuditItem> newItems = sourceKeyAndAuditItemsMap.get(sourceBean.getKey());

                    if (newItems == null) {
                        return;
                    }
                    nowSourceBeanProperty.set(sourceBean);
                    sourceAuditItemTableView.setItems(newItems);
                    sourceAuditItemTableView.refresh();
                    requestRawDataTextArea.clear();
                    responseRawDataTextArea.clear();
                }
        ));
        sourceAuditItemTableView.getSelectionModel()
                .selectedItemProperty()
                .addListener((ob, oldVal, newVal) -> {
                    SourceBean sourceBean;

                    if (newVal == null) {
                        return;
                    }
                    sourceBean = nowSourceBeanProperty.get();
                    if (sourceBean == null || !sourceBean.getKey().equals(newVal.getSourceKey())) {
                        return;
                    }
                    requestRawDataTextArea.setText(newVal.getRequestRawData());
                    responseRawDataTextArea.setText(newVal.getResponseRawData());
                });
        sourceAuditItemNameTableColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        sourceAuditItemStartAtTableColumn.setCellValueFactory(new PropertyValueFactory<>("startAt"));
        sourceAuditItemCostTableColumn.setCellValueFactory(new PropertyValueFactory<>("cost"));
        sourceAuditItemStatusTableColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        sourceAuditItemResultInfoTableColumn.setCellValueFactory(new PropertyValueFactory<>("resultInfo"));
        startAuditBtn.disableProperty().bind(loadingProperty);
        singleSourceStartAuditBtn.disableProperty().bind(loadingProperty);
        requestRawDataTextArea.setOnMouseClicked(ignored -> {
            if (requestRawDataTextArea.getLength() < 1) {
                return;
            }
            ClipboardHelper.setContent(requestRawDataTextArea.getText());
            ToastHelper.showInfoI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
        });
        responseRawDataTextArea.setOnMouseClicked(ignored -> {
            if (responseRawDataTextArea.getLength() < 1) {
                return;
            }
            ClipboardHelper.setContent(responseRawDataTextArea.getText());
            ToastHelper.showInfoI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
        });
        Platform.runLater(() -> {
            Stage stage = WindowHelper.getStage(root);

            clientInfo = getClientInfo();
            stage.setTitle(String.format(
                    I18nHelper.get(I18nKeys.SOURCE_AUDIT_WINDOW_TITLE),
                    clientInfo.getConnection().getRemoteSocketAddress().getHostName()
            ));
            stage.setOnCloseRequest(evt -> {
                destroy();
                Context.INSTANCE.popAndShowLastStage();
            });
            template.getSourceBeanList(clientInfo, sourceBeans -> {
                fillSourceBeanData(sourceBeans);
                loadingProperty.set(false);
            });
        });
    }

    private ClientInfo getClientInfo() {
        return getData();
    }

    private void destroy() {}

    private void fillSourceBeanData(List<SourceBean> sourceBeans) {
        ObservableList<SourceBean> items = sourceBeanCheckListView.getItems();

        items.clear();
        sourceKeyAndAuditItemsMap.clear();
        if (sourceBeans.isEmpty()) {
            return;
        }
        items.addAll(sourceBeans);
        sourceBeans.stream()
                .map(SourceBean::getKey)
                .forEach(sourceKey -> {
                    sourceKeyAndAuditItemsMap.put(sourceKey, FXCollections.observableArrayList(
                            SourceAuditItem.newInitializedItem(
                                    sourceKey,
                                    SourceAuditType.MOVIE_SEARCH,
                                    I18nHelper.get(I18nKeys.SOURCE_AUDIT_AUDIT_TABLE_ITEM_MOVIE_SEARCH)
                            ),
                            SourceAuditItem.newInitializedItem(
                                    sourceKey,
                                    SourceAuditType.HOME,
                                    I18nHelper.get(I18nKeys.SOURCE_AUDIT_AUDIT_TABLE_ITEM_HOME)
                            ),
                            SourceAuditItem.newInitializedItem(
                                    sourceKey,
                                    SourceAuditType.MOVIE_EXPLORE,
                                    I18nHelper.get(I18nKeys.SOURCE_AUDIT_AUDIT_TABLE_ITEM_MOVIE_EXPLORE)
                            ),
                            SourceAuditItem.newInitializedItem(
                                    sourceKey,
                                    SourceAuditType.MOVIE_DETAIL,
                                    I18nHelper.get(I18nKeys.SOURCE_AUDIT_AUDIT_TABLE_ITEM_MOVIE_DETAIL)
                            ),
                            SourceAuditItem.newInitializedItem(
                                    sourceKey,
                                    SourceAuditType.MOVIE_PLAY,
                                    I18nHelper.get(I18nKeys.SOURCE_AUDIT_AUDIT_TABLE_ITEM_MOVIE_PLAY)
                            )
                    ));
                });
    }

    @FXML
    private void onSelectAllBtnAction() {
        IndexedCheckModel<SourceBean> checkModel = sourceBeanCheckListView.getCheckModel();

        if (checkModel.getCheckedItems().size() == sourceBeanCheckListView.getItems().size()) {
            checkModel.clearChecks();
        } else {
            checkModel.checkAll();
        }
    }

    @FXML
    private void onSingleSourceStartAuditBtnAction() {
        SourceBean sourceBean = nowSourceBeanProperty.get();

        if (sourceBean == null) {
            return;
        }
        log.info("single audit operation, sourceBean={}", sourceBean.getName());
        loadingProperty.set(true);
        auditSourceBean(sourceBean, () -> loadingProperty.set(false));
    }

    @FXML
    private void onStartAuditBtnAction() {
        ObservableList<SourceBean> items = sourceBeanCheckListView.getCheckModel().getCheckedItems();

        if (items.isEmpty()) {
            return;
        }
        loadingProperty.set(true);
        nextAudit(items.iterator());
    }

    private void nextAudit(Iterator<SourceBean> sourceBeanIterator) {
        if (sourceBeanIterator.hasNext()) {
            auditSourceBean(sourceBeanIterator.next(), () -> nextAudit(sourceBeanIterator));
        } else {
            loadingProperty.set(false);
        }
    }

    private void auditSourceBean(SourceBean sourceBean, Runnable callback) {
        String sourceKey = sourceBean.getKey();

        sourceAuditExecutor.execute(SourceAuditExecutionBo.of(
                clientInfo,
                sourceBean,
                auditTypeAndRequestRawData -> {
                    SourceAuditType auditType = auditTypeAndRequestRawData.getLeft();
                    String reqRawData = auditTypeAndRequestRawData.getRight();

                    getSourceAuditItem(sourceKey, auditType)
                            .ifPresent(
                                    sourceAuditItem -> {
                                        sourceAuditItem.setRequestRawData(reqRawData);
                                        updateTextAreaIfNeeded(sourceAuditItem, reqRawData, requestRawDataTextArea);
                                    }
                            );
                },
                auditTypeAndResponseRawData -> {
                    SourceAuditType auditType = auditTypeAndResponseRawData.getLeft();
                    String respRawData = auditTypeAndResponseRawData.getRight();

                    getSourceAuditItem(sourceKey, auditType)
                            .ifPresent(
                                    sourceAuditItem -> {
                                        sourceAuditItem.setResponseRawData(respRawData);
                                        updateTextAreaIfNeeded(sourceAuditItem, respRawData, responseRawDataTextArea);
                                    }
                            );
                },
                auditTypeAndStatus -> {
                    SourceAuditType auditType = auditTypeAndStatus.getLeft();

                    sourceBeanCheckListView.getItems()
                            .stream()
                            .filter(item -> item.getKey().equals(sourceKey))
                            .findFirst()
                            .ifPresent(item -> {
                                getSourceAuditItem(sourceKey, auditType)
                                        .ifPresent(sourceAuditItem -> {
                                            SourceAuditStatus status = auditTypeAndStatus.getRight();
                                            LocalDateTime startAt;
                                            double cost;

                                            sourceAuditItem.setStatus(status);
                                            if (status == SourceAuditStatus.PROCESSING) {
                                                sourceAuditItem.setStartAt(LocalDateTime.now());
                                                sourceAuditItem.setCost(null);
                                                sourceAuditItem.setResultInfo(null);
                                            }
                                            if (
                                                    (startAt = sourceAuditItem.getStartAt()) != null &&
                                                            (
                                                                    status == SourceAuditStatus.SUCCESS ||
                                                                            status == SourceAuditStatus.FAILED
                                                            )
                                            ) {
                                                cost = ((double) startAt.until(
                                                        LocalDateTime.now(), ChronoUnit.MILLIS
                                                )) / 1000D;
                                                sourceAuditItem.setCost(cost);
                                            }
                                        });
                                if (sourceKey.equals(nowSourceBeanProperty.get().getKey())) {
                                    sourceAuditItemTableView.refresh();
                                }
                            });
                },
                auditTypeAndResults -> {
                    SourceAuditType auditType;
                    List<SourceAuditResult> results;
                    String key;

                    auditType = auditTypeAndResults.getLeft();
                    results = auditTypeAndResults.getRight();
                    if (results.isEmpty()) {
                        return;
                    }
                    key = sourceKey;
                    getSourceAuditItem(key, auditType)
                            .ifPresent(sourceAuditItem -> sourceAuditItem.setResultInfo(
                                    StringUtils.joinWith(
                                            ";",
                                            results.stream()
                                                    .map(r -> I18nHelper.get(r.getI18nKey()))
                                                    .toArray()
                                    )
                            ));
                    if (key.equals(nowSourceBeanProperty.get().getKey())) {
                        sourceAuditItemTableView.refresh();
                    }
                },
                callback,
                3
        ));
    }

    private Optional<SourceAuditItem> getSourceAuditItem(String sourceKey, SourceAuditType auditType) {
        return sourceKeyAndAuditItemsMap.get(sourceKey)
                .stream()
                .filter(sourceAuditItem -> sourceAuditItem.getAuditType() == auditType)
                .findFirst();
    }

    private void updateTextAreaIfNeeded(SourceAuditItem item, String content, TextArea textArea) {
        String sourceKey;
        SourceAuditItem sourceAuditItem;

        if (nowSourceBeanProperty.get() == null) {
            return;
        }
        sourceKey = item.getSourceKey();
        sourceAuditItem = sourceAuditItemTableView.getSelectionModel().getSelectedItem();
        if (
                !nowSourceBeanProperty.get().getKey().equals(sourceKey) ||
                sourceAuditItem == null ||
                sourceAuditItem.getAuditType() != item.getAuditType()
        ) {
            return;
        }
        textArea.setText(content);
    }
}
