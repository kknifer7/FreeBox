package io.knifer.freebox.net.aria2;

/**
 * Aria2下载器操作模板
 *
 * @author Knifer
 */
public interface Aria2Template {

    /**
     * 新增下载链接
     * @param id 任务标识
     * @param uri 下载链接
     * @return 返回结果
     */
    String addUri(String id, String uri);
}
