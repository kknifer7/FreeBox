package io.knifer.freebox.helper;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import lombok.experimental.UtilityClass;

/**
 * 剪切板工具类
 *
 * @author Knifer
 */
@UtilityClass
public class ClipboardHelper {

    public static void setContent(String value){
        setContent(DataFormat.PLAIN_TEXT, value);
    }

    public static void setContent(DataFormat dataFormat, Object value){
        Clipboard.getSystemClipboard().setContent(newClipboardContent(dataFormat, value));
    }

    private static ClipboardContent newClipboardContent(DataFormat dataFormat, Object value){
        ClipboardContent result = new ClipboardContent();

        result.put(dataFormat, value);

        return result;
    }
}
