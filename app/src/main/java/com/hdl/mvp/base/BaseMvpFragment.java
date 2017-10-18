package com.hdl.mvp.base;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * 实现mvp模式的公共Fragment类,所有使用mvp的fragment都需要继承至这个类
 * <p>
 * 温馨提示：
 * 1、实际开发中需要将BaseMvpFragment继承的Fragment改成你自己的BaseFragment（如果有）
 * 2、如果页面再进去其他页面之后不需要了，一定要及时finish
 * <p>
 * Created by HDL on 2017/10/17.
 *
 * @author HDL
 */

public abstract class BaseMvpFragment extends Fragment {
    private BasePresenter presenter = null;
    public Context mContext;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = getActivity();
        View view = View.inflate(mContext, getLayoutResId(), null);
        presenter = bindPresenter();
        initView(view);
        initData();
        return view;
    }

    /**
     * 返回资源的布局
     *
     * @return
     */
    public abstract int getLayoutResId();

    /**
     * 组件初始化操作
     *
     * @param view 父view
     */
    public abstract void initView(View view);

    /**
     * 页面初始化页面数据，在initView之后调用
     */
    public abstract void initData();

    /**
     * 绑定presenter，主要用于销毁工作
     *
     * @return
     */
    protected abstract BasePresenter bindPresenter();

    /**
     * 如果重写了此方法，一定要调用super.onDestroy();
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.onDestroy();
            presenter = null;
            System.gc();
        }
    }
}
