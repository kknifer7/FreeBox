package io.knifer.freebox.helper;

import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import lombok.experimental.UtilityClass;
import org.controlsfx.validation.ValidationMessage;
import org.controlsfx.validation.ValidationSupport;

/**
 * 验证
 *
 * @author Knifer
 */
@UtilityClass
public class ValidationHelper {

    public boolean validate(ValidationSupport support) {
        support.getRegisteredControls().forEach(c -> validate(support, c));

        return !support.isInvalid();
    }

    public boolean validate(ValidationSupport support, Control control) {
        Tooltip tooltip;

        if (support.getHighestMessage(control).isEmpty()) {
            tooltip = control.getTooltip();
            if (tooltip != null) {
                tooltip.hide();
            }

            return true;
        }
        showValidationMessage(support.getHighestMessage(control).get());

        return false;
    }

    private void showValidationMessage(ValidationMessage message){
        Control target = message.getTarget();
        Tooltip tooltip = target.getTooltip();

        if (tooltip == null) {
            tooltip = new Tooltip();
            tooltip.setAutoHide(true);
            target.setTooltip(tooltip);
            // 解除tooltip和节点的绑定，仅在代码层面控制显示和隐藏
            Tooltip.uninstall(target, tooltip);
        }
        tooltip.setText(message.getText());
        tooltip.show(target.getScene().getWindow());
    }
}
