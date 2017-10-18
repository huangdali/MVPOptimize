package com.hdl.mvp.ui.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hdl.mvp.R;
import com.hdl.mvp.base.BaseMvpActivity;
import com.hdl.mvp.base.BasePresenter;
import com.hdl.mvp.ui.MainActivity;
import com.hdl.mvp.ui.RegisteActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 登录页面
 *
 * @author HDL
 */
public class LoginActivity extends BaseMvpActivity implements LoginContact.ILoginView {
    /**
     * 用户名
     */
    private EditText etUsername;
    /**
     * 密码
     */
    private EditText etPwd;
    /**
     * 登录按钮
     */
    private Button btnLogin;
    private ProgressDialog mProgressDialog;
    private TextView tvToRegistePage;
    private LoginContact.ILoginPresenter presenter = new LoginPresenterIml(this);

    /**
     * 返回资源的布局
     *
     * @return
     */
    @Override
    public int getLayoutResId() {
        return R.layout.activity_login;
    }

    /**
     * 组件初始化操作
     */
    @Override
    public void initView() {
        etUsername = (EditText) findViewById(R.id.et_login_username);
        etPwd = (EditText) findViewById(R.id.et_login_pwd);
        tvToRegistePage = (TextView) findViewById(R.id.tv_login_to_regist);
        btnLogin = (Button) findViewById(R.id.btn_login_start);
        btnLogin.setOnClickListener(this);
        tvToRegistePage.setOnClickListener(this);
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage("登录中.....");
    }

    /**
     * 页面初始化页面数据，在initView之后调用
     */
    @Override
    public void initData() {

    }

    /**
     * 绑定presenter，主要用于销毁工作
     *
     * @return
     */
    @Override
    protected BasePresenter bindPresenter() {
        return presenter;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login_start:
                String username = getUsername();
                if (TextUtils.isEmpty(username)) {
                    etUsername.requestFocus();
                    showMsg("用户名不能为空");
                    return;
                }
                String pwd = getPwd();
                if (TextUtils.isEmpty(pwd)) {
                    etPwd.requestFocus();
                    showMsg("密码不能为空");
                    return;
                }
                presenter.startLogin(username, pwd);
                break;
            case R.id.tv_login_to_regist:
                toRegistePage();
            default:
        }
    }

    /**
     * 获取用户输入的用户名
     *
     * @return
     */
    public String getUsername() {
        return etUsername.getText().toString().trim();
    }

    /**
     * 获取用户输入并自动加密后的密码
     *
     * @return
     */
    public String getPwd() {
        String pwd = etPwd.getText().toString().trim();
        try {
            //模拟加密
            pwd = URLEncoder.encode(pwd, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return pwd;
    }

    /**
     * 显示提示消息
     *
     * @param msg
     */
    @Override
    public void showMsg(CharSequence msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示加载中
     */
    @Override
    public void showLoading() {
        mProgressDialog.show();
    }

    /**
     * 管理加载状态
     */
    @Override
    public void closeLoading() {
        mProgressDialog.dismiss();
    }

    /**
     * 跳转到主页面（登录成功之后）
     */
    @Override
    public void toMainPage() {
        startActivity(new Intent(this, MainActivity.class));
    }

    /**
     * 去注册页面
     */
    public void toRegistePage() {
        startActivity(new Intent(this, RegisteActivity.class));
    }
}
