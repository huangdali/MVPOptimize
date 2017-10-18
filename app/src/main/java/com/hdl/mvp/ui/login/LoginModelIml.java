package com.hdl.mvp.ui.login;

import android.os.Handler;
import android.os.Message;

import com.hdl.mvp.base.BaseCallbackListener;
import com.hdl.mvp.ui.result.LoginResult;

/**
 * 实际业务处理类
 * Created by HDL on 2017/10/18.
 *
 * @author HDL
 */

public class LoginModelIml implements LoginContact.ILoginModel {
    //测试代码，正式环境中不使用这个
    private BaseCallbackListener<LoginResult> callbackListener;
    //测试代码，正式环境中不使用这个
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    callbackListener.onError(new Throwable("登录失败，请检查网络连接"));
                    break;
                case 1:
                    callbackListener.onNext((LoginResult) msg.obj);
                    break;
                case 2:
                    callbackListener.onStart();
                    break;
                default:
            }
        }
    };


    /**
     * 实际登录的地方，调用服务器登录接口
     *
     * @param username         用户名
     * @param pwd              密码
     * @param callbackListener 登录结果回调
     */
    @Override
    public void login(final String username, final String pwd, final BaseCallbackListener<LoginResult> callbackListener) {
        /*
        *   1、实际开发中需要调用登录只要调用登录接口即可,如以下的登录代码:
        *   HttpSend.getInstance().login(username,pwd,callbackListener);
        *   2、下面是模拟操作，实际开发中使用类似上面的代码
         */
        //测试代码，正式环境中不使用这个
        this.callbackListener = callbackListener;
        new Thread() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(2);
                //模拟任务执行失败
                if (username.contains("e")) {
                    mHandler.sendEmptyMessage(0);
                    return;
                }
                try {
                    //模拟网速比较慢，要10s才能请求到结果
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LoginResult result = new LoginResult();
                if ("mvp".equals(username) && "132".equals(pwd)) {
                    //0表示成功
                    result.setCode("0");
                    result.setMsg("登录成功");
                } else {
                    //10002001表示用户或密码错误
                    result.setCode("10002001");
                    result.setMsg("用户名或密码错误");
                }
                Message msg = mHandler.obtainMessage();
                msg.what = 1;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        }.start();

    }
}
