package io.knifer.freebox.component.node;

import cn.hutool.core.util.ObjectUtil;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.controlsfx.control.PopOver;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * EPG - 频道节目内容弹出框
 *
 * @author Knifer
 */
public class EPGChannelProgramDataPopOver extends PopOver {

    private final Text morningContentText;
    private final Text afternoonContentText;
    private static final Supplier<String> IF_NULL_CONTENT_SUPPLIER = () -> I18nHelper.get(I18nKeys.LIVE_EPG_NO_EPG_DATA);

    public EPGChannelProgramDataPopOver() {
        super();

        TabPane root = new TabPane();
        TextFlow morningContentTextFlow = new TextFlow();
        TextFlow afternoonContentTextFlow = new TextFlow();
        ObservableList<Tab> rootTabs = root.getTabs();
        Tab morningContentTab = new Tab(I18nHelper.get(I18nKeys.LIVE_EPG_MORNING), morningContentTextFlow);
        Tab afternoonContentTab = new Tab(I18nHelper.get(I18nKeys.LIVE_EPG_AFTERNOON), afternoonContentTextFlow);

        morningContentText = new Text();
        afternoonContentText = new Text();
        morningContentText.getStyleClass().add("epg-program-data-list-content");
        afternoonContentText.getStyleClass().add("epg-program-data-list-content");
        morningContentTextFlow.getChildren().add(morningContentText);
        morningContentTextFlow.setPadding(new Insets(12));
        morningContentTextFlow.setLineSpacing(8);
        afternoonContentTextFlow.getChildren().add(afternoonContentText);
        afternoonContentTextFlow.setLineSpacing(8);
        afternoonContentTextFlow.setPadding(new Insets(12));
        morningContentTab.setClosable(false);
        morningContentTab.setGraphic(FontIcon.of(FontAwesome.SUN_O, 16, Color.ORANGE));
        afternoonContentTab.setClosable(false);
        afternoonContentTab.setGraphic(FontIcon.of(FontAwesome.MOON_O, 16, Color.BLUE));
        rootTabs.add(morningContentTab);
        rootTabs.add(afternoonContentTab);
        setOnShowing(evt -> setDetached(true));
        setContentNode(root);
    }

    public void show(Node owner, String title, @Nullable String morningContent, @Nullable String afternoonContent) {
        setTitle(title);
        morningContentText.setText(ObjectUtil.defaultIfNull(morningContent, IF_NULL_CONTENT_SUPPLIER));
        afternoonContentText.setText(ObjectUtil.defaultIfNull(afternoonContent, IF_NULL_CONTENT_SUPPLIER));
        super.show(owner);
    }
}
