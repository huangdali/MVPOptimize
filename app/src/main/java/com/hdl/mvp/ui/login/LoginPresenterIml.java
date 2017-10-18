package com.hdl.mvp.ui.login;

import android.util.Log;

import com.hdl.mvp.base.BaseCallbackListener;
import com.hdl.mvp.ui.result.LoginResult;

/**
 * 登录逻辑处理类
 * Created by HDL on 2017/10/18.
 *
 * @author HDL
 */

public class LoginPresenterIml implements LoginContact.ILoginPresenter {
    private LoginContact.ILoginView loginView;
    private LoginContact.ILoginModel loginModel;

    public LoginPresenterIml(LoginContact.ILoginView loginView) {
        this.loginView = loginView;
        loginModel = new LoginModelIml();
    }

    /**
     * 开始登录
     *
     * @param username 用户名
     * @param pwd      密码
     */
    @Override
    public void startLogin(String username, String pwd) {
        loginModel.login(username, pwd, new BaseCallbackListener<LoginResult>() {
            @Override
            public void onStart() {
                //拿到结果之后判断v层是否已经销毁，防止空对象
                if (loginView == null) {
                    Log.e("hdltag", "onStart(LoginPresenterIml.java:37):页面已经销毁，不在进行任何操作");
                    return;
                }
                loginView.showLoading();
            }

            @Override
            public void onNext(LoginResult result) {
                //拿到结果之后判断v层是否已经销毁，防止空对象
                if (loginView == null) {
                    Log.e("hdltag", "onStart(LoginPresenterIml.java:47):页面已经销毁，不在进行任何操作");
                    return;
                }
                loginView.closeLoading();
                switch (result.getCode()) {
                    case "0":
                        loginView.showMsg(result.getMsg());
                        loginView.toMainPage();
                        break;
                    case "10002001":
                        loginView.showMsg(result.getMsg());
                        break;
                    default:
                }
            }

            @Override
            public void onError(Throwable errorMsg) {
                //拿到结果之后判断v层是否已经销毁，防止空对象
                if (loginView == null) {
                    Log.e("hdltag", "onStart(LoginPresenterIml.java:67):页面已经销毁，不在进行任何操作");
                    return;
                }
                loginView.closeLoading();
                loginView.showMsg(errorMsg.getMessage());
            }
        });
    }

    /**
     * 当页面销毁的时候,需要把View=null,
     * 然后调用 System.gc();//尽管不会马上回收，只是通知jvm可以回收了，等jvm高兴就会回收
     */
    @Override
    public void onDestroy() {
        Log.e("hdltag", "onStart(LoginPresenterIml.java:82):View已经被销毁了");
        loginView = null;
        System.gc();
    }
}
