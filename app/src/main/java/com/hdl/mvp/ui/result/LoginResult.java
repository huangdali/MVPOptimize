package com.hdl.mvp.ui.result;

/**
 * 请求登录之后返回的结果（json）对应的实体
 * Created by HDL on 2017/10/18.
 */

public class LoginResult {
    private String code;
    private String msg;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "LoginResult{" +
                "code='" + code + '\'' +
                ", msg='" + msg + '\'' +
                '}';
    }
}
