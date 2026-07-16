package io.knifer.freebox.util;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import lombok.experimental.UtilityClass;

/**
 * JavaFX Node相关工具类
 *
 * @author Knifer
 */
@UtilityClass
public class NodeUtil {

    /**
     * 以class新值替换组件的某个旧值
     * @param node 组件
     * @param oldClass 旧值
     * @param newClass 新值
     */
    public void replaceStyleClass(Node node, String oldClass, String newClass) {
        ObservableList<String> classes = node.getStyleClass();

        classes.remove(oldClass);
        classes.add(newClass);
    }
}
