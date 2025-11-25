package io.knifer.freebox.controller;

import com.google.common.base.Charsets;
import io.knifer.freebox.component.factory.SourceBeanCheckListCellFactory;
import io.knifer.freebox.component.factory.SourceBeanProblemListCellFactory;
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
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.domain.SourceAuditItem;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditExecutor;
import io.knifer.freebox.service.sourceaudit.auditor.impl.SourceAuditExecutorImpl;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.util.CastUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.IndexedCheckModel;
import org.controlsfx.control.tableview2.TableColumn2;
import org.controlsfx.control.tableview2.TableView2;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 源审计
 *
 * @author Knifer
 */
@Slf4j
public class SourceAuditController implements Destroyable {

    @FXML
    private BorderPane root;
    @FXML
    private CheckListView<SourceBean> sourceBeanCheckListView;
    @FXML
    private ListView<SourceBean> problemSourceListView;
    @FXML
    private Label nowSourceBeanLabel;
    @FXML
    private ProgressIndicator auditingProgressIndicator;
    @FXML
    private Label auditingSourceBeanLabel;
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
    @FXML
    private Button stopAuditingSourceBeanBtn;

    private SpiderTemplate template;
    private ClientManager clientManager;
    private SourceAuditExecutor sourceAuditExecutor;
    private final SimpleBooleanProperty interruptAuditFlag = new SimpleBooleanProperty(true);
    private final SimpleObjectProperty<SourceBean> nowSourceBeanProperty = new SimpleObjectProperty<>();
    private final BooleanProperty loadingProperty = new SimpleBooleanProperty(true);
    private final Map<String, ObservableList<SourceAuditItem>> sourceKeyAndAuditItemsMap = new HashMap<>();

    @FXML
    private void initialize() {
        template = Context.INSTANCE.getSpiderTemplate();
        clientManager = Context.INSTANCE.getClientManager();
        sourceAuditExecutor = new SourceAuditExecutorImpl(template);

        nowSourceBeanProperty.addListener((ob, oldVal, newVal) -> {
            if (newVal != null) {
                nowSourceBeanLabel.setText(I18nHelper.getFormatted(
                        I18nKeys.SOURCE_AUDIT_NOW, newVal.getName()
                ));
            }
        });
        sourceBeanCheckListView.setCellFactory(new SourceBeanCheckListCellFactory(
                sourceBeanCheckListView,
                this::updateNowSourceBean
        ));
        problemSourceListView.setCellFactory(new SourceBeanProblemListCellFactory(this::updateNowSourceBean));
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
                    dealWithRawData(requestRawDataTextArea, newVal.getRequestRawData());
                    dealWithRawData(responseRawDataTextArea, newVal.getResponseRawData());
                });
        sourceAuditItemNameTableColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        sourceAuditItemStartAtTableColumn.setCellValueFactory(new PropertyValueFactory<>("startAt"));
        sourceAuditItemCostTableColumn.setCellValueFactory(new PropertyValueFactory<>("cost"));
        sourceAuditItemStatusTableColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        sourceAuditItemResultInfoTableColumn.setCellValueFactory(new PropertyValueFactory<>("resultInfo"));
        startAuditBtn.disableProperty().bind(loadingProperty);
        singleSourceStartAuditBtn.disableProperty().bind(loadingProperty);
        auditingProgressIndicator.visibleProperty().bind(loadingProperty);
        auditingSourceBeanLabel.visibleProperty().bind(loadingProperty);
        stopAuditingSourceBeanBtn.visibleProperty().bind(loadingProperty);
        stopAuditingSourceBeanBtn.disableProperty().bind(interruptAuditFlag);
        Platform.runLater(() -> {
            Stage stage = WindowHelper.getStage(root);
            ClientInfo clientInfo = clientManager.getCurrentClientImmediately();

            if (clientInfo != null) {
                stage.setTitle(String.format(
                        I18nHelper.get(I18nKeys.SOURCE_AUDIT_WINDOW_TITLE),
                        clientInfo.getName()
                ));
            }
            stage.setOnCloseRequest(evt -> {
                destroy();
                Context.INSTANCE.popAndShowLastStage();
            });
            template.init(success -> {
                if (!success) {
                    return;
                }
                template.getSourceBeanList(sourceBeans ->
                    Platform.runLater(() -> {
                        fillSourceBeanData(sourceBeans);
                        interruptAuditFlag.set(false);
                        setLoading(false);
                    })
                );
            });
        });
    }

    private void dealWithRawData(TextArea rawDataTextArea, String rawData) {
        List<String> styleClasses = rawDataTextArea.getStyleClass();

        rawDataTextArea.setUserData(rawData);
        styleClasses.remove("gray");
        if (StringUtils.getBytes(rawData, Charsets.UTF_8).length > 102400) {
            rawDataTextArea.setText(I18nHelper.get(I18nKeys.SOURCE_AUDIT_RAW_DATA_TOO_LONG));
            styleClasses.add("gray");
        } else {
            rawDataTextArea.setText(rawData);
        }
    }

    private void updateNowSourceBean(@Nullable SourceBean sourceBean) {
        ObservableList<SourceAuditItem> newItems;

        if (sourceBean == null) {
            return;
        }
        newItems = sourceKeyAndAuditItemsMap.get(sourceBean.getKey());
        if (newItems == null) {
            return;
        }
        nowSourceBeanProperty.set(sourceBean);
        sourceAuditItemTableView.setItems(newItems);
        sourceAuditItemTableView.refresh();
        requestRawDataTextArea.clear();
        responseRawDataTextArea.clear();
    }

    @Override
    public void destroy() {
        onStopAuditingSourceBeanBtnAction();
        clientManager.clearCurrentClient();
    }

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
                .forEach(sourceKey ->
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
                    ))
                );
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
        problemSourceListView.getItems().remove(sourceBean);
        setLoading(true, sourceBean);
        auditSourceBean(sourceBean, () -> {
            if (interruptAuditFlag.get()) {
                interruptAuditFlag.set(false);
            }
            setLoading(false);
        });
    }

    @FXML
    private void onStartAuditBtnAction() {
        List<SourceBean> items = sourceBeanCheckListView.getCheckModel()
                .getCheckedItems()
                .stream()
                .toList();

        if (items.isEmpty()) {
            return;
        }
        log.info(
                "batch audit operation, sourceBeans={}",
                items.stream().map(SourceBean::getName).collect(Collectors.joining(","))
        );
        problemSourceListView.getItems().removeAll(items);
        setLoading(true);
        ToastHelper.disableErrorShow();
        nextAudit(items.iterator());
    }

    private void nextAudit(Iterator<SourceBean> sourceBeanIterator) {
        SourceBean sourceBean;

        if (sourceBeanIterator.hasNext() && !interruptAuditFlag.get()) {
            sourceBean = sourceBeanIterator.next();
            updateAuditingLabel(sourceBean);
            auditSourceBean(sourceBean, () -> nextAudit(sourceBeanIterator));
        } else {
            interruptAuditFlag.set(false);
            setLoading(false);
            ToastHelper.enableErrorShow();
        }
    }

    private void setLoading(boolean flag) {
        setLoading(flag, null);
    }

    private void setLoading(boolean flag, @Nullable SourceBean sourceBean) {
        loadingProperty.set(flag);
        if (!flag) {

            return;
        }
        if (sourceBean != null) {
            updateAuditingLabel(sourceBean);
        }
    }

    private void updateAuditingLabel(SourceBean sourceBean) {
        Platform.runLater(() -> auditingSourceBeanLabel.setText(
                I18nHelper.getFormatted(I18nKeys.SOURCE_AUDIT_AUDITING, sourceBean.getName()))
        );
    }

    private void auditSourceBean(SourceBean sourceBean, Runnable callback) {
        String sourceKey = sourceBean.getKey();

        sourceAuditExecutor.execute(SourceAuditExecutionBo.of(
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
                    boolean problemFlag;
                    Collection<SourceBean> problemItems;

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
                    problemFlag = results.stream()
                            .anyMatch(r -> r.getLevel() == SourceAuditResult.Level.ERROR);
                    problemItems = problemSourceListView.getItems();
                    if (problemFlag && !problemItems.contains(sourceBean)) {
                        problemItems.add(sourceBean);
                    }
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

    @FXML
    private void onRawDataTextAreaClick(MouseEvent evt) {
        TextArea rawDataTextArea = (TextArea) evt.getSource();

        if (rawDataTextArea.getLength() < 1 || evt.getButton() != MouseButton.PRIMARY) {
            return;
        }
        ClipboardHelper.setContent(CastUtil.cast(rawDataTextArea.getUserData()));
        ToastHelper.showInfoI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
    }

    @FXML
    private void onStopAuditingSourceBeanBtnAction() {
        sourceAuditExecutor.stop();
        interruptAuditFlag.set(true);
    }
}
