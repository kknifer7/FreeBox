package io.knifer.freebox.component.factory;

import io.knifer.freebox.model.domain.LiveChannel;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import org.apache.commons.lang3.tuple.Pair;

/**
 * EPG频道 - 可搜索下拉框单元格工厂
 *
 * @author Knifer
 */
public class LiveChannelSearchableComboBoxCellFactory implements
        Callback<ListView<Pair<String, LiveChannel>>, ListCell<Pair<String, LiveChannel>>> {

    @Override
    public ListCell<Pair<String, LiveChannel>> call(ListView<Pair<String, LiveChannel>> pairListView) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Pair<String, LiveChannel> groupTitleAndChannelPair, boolean empty) {
                String groupTitle;
                LiveChannel channel;

                super.updateItem(groupTitleAndChannelPair, empty);
                if (
                        groupTitleAndChannelPair == null ||
                                empty ||
                                (groupTitle = groupTitleAndChannelPair.getLeft()) == null ||
                                (channel = groupTitleAndChannelPair.getRight()) == null
                ) {
                    setText(null);
                    setGraphic(null);

                    return;
                }
                setText(channel.getTitle() + " - " + groupTitle);
            }
        };
    }
}
