package io.knifer.freebox.component.router;

import com.google.inject.Singleton;
import io.knifer.freebox.controller.BaseController;
import io.knifer.freebox.util.CastUtil;
import jakarta.inject.Inject;
import javafx.stage.Stage;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 窗口路由
 * 仅在FX线程中使用
 *
 * @author Knifer
 */
@Singleton
public class Router {

    /**
     * 主要窗口栈
     * 维护了所有创建出的主要窗口，类似浏览器中可以进行回退的窗口栈，务必确保它得到正确维护，这关系到应用程序能否正常退出
     */
    private final Stack<Stage> mainStageStack;

    /**
     * 次要窗口+控制器 map
     * @see io.knifer.freebox.constant.Views
     */
    private final Map<String, Pair<Stage, ? extends BaseController>> secondaryStageAndControllerMap;

    /**
     * FX主Stage
     */
    @Getter
    private final Stage primary;

    /**
     * 当前Stage
     */
    private Stage current;

    @Inject
    public Router(Stage primary) {
        this.primary = primary;
        this.mainStageStack = new Stack<>();
        this.secondaryStageAndControllerMap = new HashMap<>();
    }

    public void route(Stage currentStage, Stage nextStage) {
        push(currentStage);
        this.current = nextStage;
        currentStage.hide();
        if (currentStage == primary && !secondaryStageAndControllerMap.isEmpty()) {
            // 当前窗口是主窗口时，一旦主窗口隐藏，次要窗口会随之隐藏，因此需要手动将次要窗口重新show出来
            secondaryStageAndControllerMap.values().forEach(pair ->
                pair.getLeft().show()
            );
        }
        nextStage.show();
    }

    private void push(Stage stage) {
        if (mainStageStack.contains(stage)) {

            return;
        }
        mainStageStack.push(stage);
    }

    public void back() {
        Stage stage = pop();

        if (stage != null) {
            stage.show();
        }
    }

    @Nullable
    public Stage pop() {
        if (mainStageStack.isEmpty()) {

            return null;
        }

        return mainStageStack.pop();
    }

    public List<Stage> popAll() {
        List<Stage> stageList;

        if (mainStageStack.isEmpty()) {

            return List.of();
        }
        stageList = new ArrayList<>(mainStageStack.size());
        while (!mainStageStack.isEmpty()) {
            stageList.add(mainStageStack.pop());
        }

        return stageList;
    }

    /**
     * 存放次要窗口+控制器
     * @param viewName 名称
     * @param stageAndController 窗口+控制器
     * @see io.knifer.freebox.constant.Views
     */
    public void putSecondary(String viewName, Pair<Stage, ? extends BaseController> stageAndController) {
        secondaryStageAndControllerMap.put(viewName, stageAndController);
    }

    /**
     * 移除次要窗口+控制器
     * @param viewName 窗口名称
     * @see io.knifer.freebox.constant.Views
     */
    public void removeSecondary(String viewName) {
        secondaryStageAndControllerMap.remove(viewName);
    }

    /**
     * 获取次要窗口+控制器
     * @param viewName 窗口名称
     * @return 窗口+控制器
     * @see io.knifer.freebox.constant.Views
     */
    @Nullable
    public <T extends BaseController> Pair<Stage, T> getSecondary(String viewName) {
        return CastUtil.cast(secondaryStageAndControllerMap.get(viewName));
    }

    /**
     * 获取所有次要窗口+控制器
     * @return 所有 窗口+控制器
     */
    public Collection<Pair<Stage, ? extends BaseController>> getSecondaries() {
        return secondaryStageAndControllerMap.values();
    }

    public Stage getCurrent() {
        return current == null ? primary : current;
    }

    public void resetCurrent() {
        current = primary;
    }
}
