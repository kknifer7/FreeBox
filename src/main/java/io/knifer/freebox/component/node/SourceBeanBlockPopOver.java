package io.knifer.freebox.component.node;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Sets;
import io.knifer.freebox.component.factory.SourceBeanCheckListCellFactory;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.StorageHelper;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.domain.SourceBeanBlockList;
import io.knifer.freebox.util.CastUtil;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.IndexedCheckModel;
import org.controlsfx.control.PopOver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 源屏蔽弹出框
 *
 * @author Knifer
 */
public class SourceBeanBlockPopOver extends PopOver {

    private final CheckListView<SourceBean> checkListView;
    private final List<SourceBean> blockedSourceBeans;
    private final ClientInfo clientInfo;

    public SourceBeanBlockPopOver(Consumer<List<SourceBean>> onBlock) {
        super();
        Button okBtn = new Button(I18nHelper.get(I18nKeys.TV_BLOCK));
        Button resetBtn = new Button(I18nHelper.get(I18nKeys.COMMON_RESET));
        HBox btnHBox;
        VBox root;
        List<SourceBean> items;
        ObservableList<SourceBean> checkedItems;

        setTitle(I18nHelper.get(I18nKeys.TV_SOURCE_BEAN_BLOCK));
        setOnShowing(evt -> setDetached(true));
        clientInfo = Context.INSTANCE.getClientManager().getCurrentClientImmediately();
        if (clientInfo == null) {
            throw new IllegalStateException();
        }
        blockedSourceBeans = new ArrayList<>();
        checkListView = new CheckListView<>();
        checkListView.setFocusTraversable(false);
        checkListView.setCellFactory(new SourceBeanCheckListCellFactory(checkListView, ignored -> {}));
        items = checkListView.getItems();
        checkedItems = checkListView.getCheckModel().getCheckedItems();
        checkedItems.addListener((ListChangeListener<SourceBean>) ignored -> {
            boolean okBtnDisableFlag = checkedItems.size() == items.size();

            if (okBtn.disableProperty().get() != okBtnDisableFlag) {
                okBtn.setDisable(okBtnDisableFlag);
            }
        });
        resetBtn.setFocusTraversable(false);
        resetBtn.setOnAction(evt -> checkListView.getCheckModel().clearChecks());
        okBtn.setFocusTraversable(false);
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(evt -> {
            if (!blockedSourceBeans.equals(checkedItems)) {
                blockedSourceBeans.clear();
                blockedSourceBeans.addAll(checkedItems);
                StorageHelper.save(SourceBeanBlockList.of(clientInfo.getId(), blockedSourceBeans));
                onBlock.accept(CastUtil.cast(CollUtil.subtract(items, blockedSourceBeans)));
            }
            hide();
        });
        btnHBox = new HBox(resetBtn, okBtn);
        btnHBox.setSpacing(3);
        btnHBox.setAlignment(Pos.BASELINE_RIGHT);
        root = new VBox(checkListView, btnHBox);
        root.setPadding(new Insets(10, 10, 10, 10));
        root.setSpacing(5);
        setContentNode(root);
    }

    /**
     * 设置源列表，返回屏蔽过滤后的源列表
     * @param sourceBeans 源列表
     * @return 屏蔽过滤后的源列表
     */
    public List<SourceBean> setSourceBeans(List<SourceBean> sourceBeans) {
        ObservableList<SourceBean> items = checkListView.getItems();
        Set<String> blockedSourceBeanKeys;
        IndexedCheckModel<SourceBean> checkModel;

        items.clear();
        blockedSourceBeans.clear();
        if (sourceBeans.isEmpty()) {

            return items;
        }
        items.addAll(sourceBeans);
        blockedSourceBeanKeys = loadBlockedSourceBeanKeys();
        if (blockedSourceBeanKeys.isEmpty()) {

            return items;
        }
        checkModel = checkListView.getCheckModel();
        sourceBeans.forEach(sourceBean -> {
            if (blockedSourceBeanKeys.contains(sourceBean.getKey())) {
                checkModel.check(sourceBean);
                blockedSourceBeans.add(sourceBean);
            }
        });

        return CastUtil.cast(CollUtil.subtract(items, checkModel.getCheckedItems()));
    }

    private Set<String> loadBlockedSourceBeanKeys() {
        SourceBeanBlockList sourceBeanBlockList = StorageHelper.find(
                clientInfo.getId(), SourceBeanBlockList.class
        ).orElse(null);

        if (sourceBeanBlockList == null) {
            return Set.of();
        }

        return Sets.newHashSet(sourceBeanBlockList.getSourceBeanKeys());
    }
}
