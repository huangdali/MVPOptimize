# MVPOptimize MVP模式优化

> 主要优化P层V层互相持有对象，不能及时回收/销毁问题

如果你看过我的MVP整个教程[【android进阶篇】MVP+Retrofit+RxJava框架结合](http://blog.csdn.net/qq137722697/article/details/52212348) 你可能就会发现，如果页面在请求的时候，网络不好，这时用户跳转到其他页面，就可能会抛出空指针异常/空对象/内存泄露等问题；

## 内存泄露追踪

### 手动调用finish();方法销毁当前页面

如果你的业务中在进入下一个页面时，会把当前页面销毁，此时你会调用finish();方法，但是其实是有问题，来看看这张MVP结构图：

![](http://img.blog.csdn.net/20160815164604370)

M 层与 P 层是互相持有对象的关系；通过代码也可以看出来：

View层持有Presenter层：

```java
public class LoginActivity extends BaseMvpActivity implements LoginContact.ILoginView {
    ...
    private LoginContact.ILoginPresenter presenter;
    ...
 }
```

Presenter层持有View层对象：
```java
public class LoginPresenterIml implements LoginContact.ILoginPresenter {
    ...
    private LoginContact.ILoginView loginView;
    ...
}
```

为什么要表明他们互相持有呢？

因为即使页面finish之后，如果Presenter层还持有View层的引用，jvm不会马上回收，也就是说finish之后页面没有真正意思上的销毁；

那么问题就来了，既然没有销毁，如果这样没有销毁的页面太多，就会造成**内存泄露**；

### 解决建议

既然finish();之后不会真正销毁是因为它们互相持有对象，那就把这种关系打破即可，怎么做呢？

- 1、在页面（activity/fragment）回调onDestroy()方法的时候，通知presenter断开持有View层的引用；并将presenter对象赋值为null,调用System.gc();通知jvm；

- 2、presenter在收到通知销毁页面的时候，将view赋值为null，并且调用System.gc();通知jvm

    - [推荐] 在presenter收到通知销毁页面的时候，有条件的话可以增加一个取消正在执行任务的方法（此方法非必须，继续往下看）；

特别说明：
- 将对象赋值为null，会断开对象的引用；
    - 将presenter赋值为null，会断开View持有Presenter对象的引用

    - 同样的将view赋值为null，会断开presenter持有view对象的引用

- 调用System.gc()通知jvm可以回收垃圾了，jvm并不会马上回收，待jvm"心情好"的时候会自动回收；

来看看代码实现：（后面有完整代码）

1、view的onDestroy方法通知销毁页面，并设置presenter为null

```java
@Override
    public void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.onDestroy();
            presenter = null;
            System.gc();
        }
    }
```


2、presenter解除持有view对象

```java
public class LoginPresenterIml implements LoginContact.ILoginPresenter {
    ...

    /**
     * 当页面销毁的时候,需要把View=null,
     * 然后调用 System.gc();//尽管不会马上回收，只是通知jvm可以回收了，等jvm高兴就会回收
     */
    @Override
    public void onDestroy() {
        loginView = null;
        System.gc();
    }
}
```

>以上操作是重复代码，考虑抽取，继续往下看


### 空指针/空对象问题

#### 场景一
通过以上两个操作，jvm会适时回收view层，但是如果presenter层还在继续做耗时操作的话不会马上被回收，此时如果view已经被回收，耗时操作刚好完成要通知view层做更新ui的操作，那么就会出现空指针/空对象的异常；

比如：获取网络数据（耗时操作）成功后，需要展示到ui上，此时会调用view.showData(result)方法，view对象为空，就抛出异常了；

#### 场景二
还有一种情况也会出现空指针/空对象的问题，就是jvm在内存吃紧的时候会回收不可见的view；你可能会说上面不是在页面（activity/fragment）的onDestory()方法调用的时候通知销毁对象嘛！

其实，jvm在回收页面的时候不会保证回调onDestroy方法的，所以就不能及时通知presenter销毁了。

#### 解决建议
这个问题很好解决：presenter中，在所有需要使用view对象之前加一个非空判断，如果为空直接return；不在做任何操作

例子：

```java
public class LoginPresenterIml implements LoginContact.ILoginPresenter {
   ...

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
}

```

## 总结

通过以上两个方法基本上可以避免大部分mvp中的内存泄露和空指针异常问题，对于一些重复的操作我们可以这样抽取一下：

既然每个页面都需要在onDestroy方法中通知presenter销毁对象，那就抽取一个公共的父类；

### BasePresenter

每个presenter对象都有销毁对象的方法,所以抽取一个公共的接口类BasePresenter并定义个onDestroy方法，以后所有的presenter都需要实现这个接口

```java
/**
 * 公共Presenter类
 * Created by HDL on 2017/10/17.
 *
 * @author HDL
 */

public interface BasePresenter {
    /**
     * 当页面销毁的时候,需要把View=null,
     * 然后调用 System.gc();//尽管不会马上回收，只是通知jvm可以回收了，等jvm高兴就会回收
     */
    void onDestroy();
}

```

### BaseMvpActivity

继续来抽取一下每个页面都需要调用的onDestroy()方法,所以抽取一个公共的BaseMvpActivity类，这个类重写onDestroy方法，在这个方法中实现调用presenter.onDestory()和断开presenter引用的操作；

>实际开发中如果你的项目中已经有BaseActivity了，那只需要将BaseMvpActivity继承的AppCompatActivity改成你自己的BaseActivity即可


```java
/**
 * 实现mvp模式的公共activity类，所有使用mvp的activity页面都需要继承此类
 * <p>
 * 温馨提示：
 * 1、实际开发中需要将BaseMvpActivity继承的AppCompatActivity改成你自己的BaseActivity（如果有）
 * 2、如果页面再进去其他页面之后不需要了，一定要及时finish
 * <p>
 * Created by HDL on 2017/10/17.
 *
 * @author HDL
 */

public abstract class BaseMvpActivity extends AppCompatActivity implements View.OnClickListener {
    private BasePresenter presenter = null;
    public Context mContext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        mContext = this;
        presenter = bindPresenter();
        initView();
        initData();
    }


    /**
     * 返回资源的布局
     *
     * @return
     */
    public abstract int getLayoutResId();

    /**
     * 组件初始化操作
     */
    public abstract void initView();

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
```

里面的getLayoutResId()、initView()、initData()方法都是为了规范页面的写法而抽取的（非必须抽取），如果觉得麻烦可以不抽取；

- BaseMvpFragment

同理Fragment也可以抽取一下

```java
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

```

公共的方法抽取好之后来看一个登录的例子：

### 目录结构

![](https://github.com/huangdali/mvpoptimize/master/blob/image/struct.png)

### 登录界面
![](https://github.com/huangdali/mvpoptimize/master/blob/image/login.png)


### 接口管理类LoginContact：

```java
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

```

### 实际业务处理类(Model层)LoginModelIml：


```java
/**
 * 实际业务处理类
 * Created by HDL on 2017/10/18.
 *
 * @author HDL
 */

public class LoginModelIml implements LoginContact.ILoginModel {
    /**
     * 实际登录的地方，调用服务器登录接口
     *
     * @param username         用户名
     * @param pwd              密码
     * @param callbackListener 登录结果回调
     */
    @Override
    public void login(final String username, final String pwd, final BaseCallbackListener<LoginResult> callbackListener) {
     //登录请求
      HttpSend.getInstance().login(username,pwd,callbackListener);
    }
}

```

由于没有测试环境，所以来模拟登录的过程，改造一下

```java
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

```

### 实际逻辑处理类（Presenter层）LoginPresenterIml:

```java
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

```

### View层LoginActivity:

```java
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

```

### 输出结果：
![](https://github.com/huangdali/mvpoptimize/master/blob/image/out.png)

Demo地址：https://github.com/huangdali/mvpoptimize/












