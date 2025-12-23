package io.knifer.freebox.component.factory;

import io.knifer.freebox.component.node.EmojiableLabel;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.List;

/**
 * 源对象列表项工厂（支持emoji）
 *
 * @author knifer
 */
public class SourceBeanListCellFactory implements Callback<ListView<SourceBean>, ListCell<SourceBean>> {
    
    @Override
    public ListCell<SourceBean> call(ListView<SourceBean> listView) {
        return new ListCell<>() {

            private final EmojiableLabel emojiLabel;
            
            {
                List<String> styleClasses;

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                emojiLabel = new EmojiableLabel();
                styleClasses = emojiLabel.getStyleClass();
                styleClasses.add("fse-medium");
            }
            
            @Override
            protected void updateItem(SourceBean item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    emojiLabel.setText(item.getName());
                    setGraphic(emojiLabel);
                }
            }
        };
    }
}