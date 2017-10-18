package com.hdl.mvp.base;

/**
 * 公共回调监听
 * Created by HDL on 2017/10/18.
 *
 * @author HDL
 */

public interface BaseCallbackListener<T> {
    /**
     * 当任务开始的时候回调
     */
    void onStart();

    /**
     * 当任务成功的时候回调
     *
     * @param result 任务请求结果
     */
    void onNext(T result);

    /**
     * 当任务执行过程中出错的时候回调
     *
     * @param errorMsg 错误消息
     */
    void onError(Throwable errorMsg);
}
