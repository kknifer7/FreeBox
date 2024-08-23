package io.knifer.freebox.constant;

import lombok.experimental.UtilityClass;

/**
 * 返回码
 *
 * @author Knifer
 */
@UtilityClass
public class MessageCodes {

    /**
     * 注册
     */
    public static final int REGISTER = 100;
    /**
     * 获取源列表
     */
    public static final int GET_SOURCE_BEAN_LIST = 201;
    /**
     * 获取源列表结果
     */
    public static final int GET_SOURCE_BEAN_LIST_RESULT = 202;
    /**
     * 获取首页信息
     */
    public static final int GET_HOME_CONTENT = 203;
    /**
     * 获取首页信息结果
     */
    public static final int GET_HOME_CONTENT_RESULT = 204;
    /**
     * 获取指定分类信息
     */
    public static final int GET_CATEGORY_CONTENT = 205;
    /**
     * 获取指定分类信息结果
     */
    public static final int GET_CATEGORY_CONTENT_RESULT = 206;
}
