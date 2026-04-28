package io.knifer.freebox.controller.spiderDebugging;

import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.controller.SpiderDebuggingController;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TabPane;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * 爬虫调试 tab controller 接口
 * @author Knifer
 */
public abstract class SpiderDebuggingTabController {

    protected SpiderDebuggingController parentController;
    protected BooleanProperty spiderLoadingProperty;
    protected TabPane previewTabPane;
    protected Map<SourceAuditType, Future<?>> spiderPreviewTaskMap;
    protected ExecutorService spiderPreviewExecutor;

    /**
     * 应用父Controller
     */
     protected void applyParentController(SpiderDebuggingController parentController) {
        this.parentController = parentController;
        this.previewTabPane = parentController.getPreviewTabPane();
        this.spiderLoadingProperty = parentController.getSpiderLoadingProperty();
        this.spiderPreviewTaskMap = parentController.getSpiderPreviewTaskMap();
        this.spiderPreviewExecutor = parentController.getSpiderPreviewExecutor();
    }

    /**
     * 刷新数据
     */
    public abstract void reload();

    /**
     * 清空数据和UI状态
     */
    public abstract void clear();

    /**
     * 获取当前tab的加载状态属性
     * @return 当前tab的加载状态属性
     */
    public abstract BooleanProperty getLoadingProperty();

    public boolean isAutoRefreshOn() {
        return false;
    }

    public static int HOME_TAB_IDX = 0;
    public static int MOVIE_EXPLORE_TAB_IDX = 1;
    public static int MOVIE_DETAIL_TAB_IDX = 2;
}
