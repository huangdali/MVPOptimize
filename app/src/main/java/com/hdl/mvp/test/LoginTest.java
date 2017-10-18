package com.hdl.mvp.test;

import com.hdl.mvp.base.BaseCallbackListener;
import com.hdl.mvp.ui.login.LoginModelIml;
import com.hdl.mvp.ui.result.LoginResult;

import org.junit.Test;


/**
 * 登录单元测试
 * Created by HDL on 2017/10/18.
 */

public class LoginTest {
    @Test
    public void testLogin() {
        new LoginModelIml().login("mvpe", "132", new BaseCallbackListener<LoginResult>() {
            @Override
            public void onStart() {
                System.out.println("任务开始了");
            }

            @Override
            public void onNext(LoginResult result) {
                System.out.println("任务成功了 " + result);
            }

            @Override
            public void onError(Throwable errorMsg) {
                System.out.println("任务出错了 " + errorMsg);
            }
        });
    }
}
