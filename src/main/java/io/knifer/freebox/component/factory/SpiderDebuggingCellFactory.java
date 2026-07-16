package io.knifer.freebox.component.factory;

import io.knifer.freebox.model.domain.SpiderDebugging;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

/**
 * 调试爬虫单元格工厂
 *
 * @author Knifer
 */
public class SpiderDebuggingCellFactory implements Callback<ListView<SpiderDebugging>, ListCell<SpiderDebugging>> {

    @Override
    public ListCell<SpiderDebugging> call(ListView<SpiderDebugging> spiderDebuggingListView) {
        return new SpiderDebuggingListCell();
    }
}
