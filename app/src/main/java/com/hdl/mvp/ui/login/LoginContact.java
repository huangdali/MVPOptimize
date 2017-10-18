package com.hdl.mvp.ui.login;

import com.hdl.mvp.base.BaseCallbackListener;
import com.hdl.mvp.base.BasePresenter;
import com.hdl.mvp.ui.result.LoginResult;

/**
 * MVP接口管理类
 * Created by HDL on 2017/10/18.
 *
 * @author HDL
 */

public class LoginContact {
    /**
     * M层
     */
    public interface ILoginModel {
        /**
         * 实际登录的地方，调用服务器登录接口
         *
         * @param username         用户名
         * @param pwd              密码
         * @param callbackListener 登录结果回调
         */
        void login(String username, String pwd, BaseCallbackListener<LoginResult> callbackListener);
    }

    /**
     * V层
     */
    public interface ILoginView {
        /**
         * 显示提示消息
         *
         * @param msg
         */
        void showMsg(CharSequence msg);

        /**
         * 显示加载中
         */
        void showLoading();

        /**
         * 管理加载状态
         */
        void closeLoading();

        /**
         * 跳转到主页面（登录成功之后）
         */
        void toMainPage();
    }

    /**
     * P层
     */
    public interface ILoginPresenter extends BasePresenter{
        /**
         * 开始登录
         *
         * @param username 用户名
         * @param pwd      密码
         */
        void startLogin(String username, String pwd);
    }
}
