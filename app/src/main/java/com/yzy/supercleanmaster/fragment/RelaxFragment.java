package com.yzy.supercleanmaster.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.yzy.supercleanmaster.R;
import com.yzy.supercleanmaster.base.BaseFragment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class RelaxFragment extends BaseFragment {

    //@InjectView(R.id.webView)
    WebView mWebView;
    //@InjectView(R.id.progress_bar)
    ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeLayout;
    Context mContext;

    @Override
    protected View initViews(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    //public View onCreateView(LayoutInflater inflater,
    //                         @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        hookWebView();
        View view = inflater.inflate(R.layout.fragment_relax, container, false);
        //ButterKnife.inject(this, view);

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mWebView = (WebView) view.findViewById(R.id.webView);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mContext = getActivity();


        Log.i("CleanMaster", "RelaxFragment.initViews()");

        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //initWebview();
        Log.i("CleanMaster", "RelaxFragment.onViewCreated");
    }

    @Override
    protected void initData() {
        Log.i("CleanMaster", "RelaxFragment.initData() called");
        initWebview();
    }

    @Override
    public boolean getUserVisibleHint() {
        Log.i("CleanMaster", "RelaxFragment.getUserVisibleHint() called");
        return super.getUserVisibleHint();
    }

    private void fillData() {

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //ButterKnife.reset(this);
        mWebView.removeAllViews();
        mWebView.destroy();
        Log.i("CleanMaster", "RelaxFragment.onDestroyView");
    }
    private void initWebview() {
        // TODO Auto-generated method stub
        Log.i("CleanMaster", "RelaxFragment.initWebview called");
        mSwipeLayout.setOnRefreshListener(()->mWebView.loadUrl(mWebView.getUrl()));//重新刷新页面
        mSwipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_red_light);

        initWebSettings();

        mWebView.requestFocusFromTouch();//支持获取手势焦点
        // 访问assets目录下的文件

        mWebView.loadUrl("http://girl-atlas.net");
        mWebView.requestFocus();//触摸焦点起作用

        // 设置WebViewClient
        mWebView.setWebViewClient(new WebViewClient() {
            // url拦截
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 使用自己的WebView组件来响应Url加载事件，而不是使用默认浏览器器加载页面
                view.loadUrl(url);
                // 相应完成返回true
                return true;
                // return super.shouldOverrideUrlLoading(view, url);
            }

            // 页面开始加载
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mProgressBar.setVisibility(View.VISIBLE);
                super.onPageStarted(view, url, favicon);
            }

            // 页面加载完成
            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.GONE);
                super.onPageFinished(view, url);
            }

            // WebView加载的所有资源url
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

        });

        // 设置WebChromeClient
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            // 处理javascript中的alert
            public boolean onJsAlert(WebView view, String url, String message,
                                     final JsResult result) {
                return super.onJsAlert(view, url, message, result);
            };

            @Override
            // 处理javascript中的confirm
            public boolean onJsConfirm(WebView view, String url,
                                       String message, final JsResult result) {
                return super.onJsConfirm(view, url, message, result);
            };

            @Override
            // 处理javascript中的prompt
            public boolean onJsPrompt(WebView view, String url, String message,
                                      String defaultValue, final JsPromptResult result) {
                return super.onJsPrompt(view, url, message, defaultValue,
                        result);
            };

            // 设置网页加载的进度条
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgressBar.setProgress(newProgress);
                if (100 == newProgress) {
                    mSwipeLayout.setRefreshing(false);//隐藏进度条
                } else {
                    if (!mSwipeLayout.isRefreshing()) mSwipeLayout.setRefreshing(true);
                }
                super.onProgressChanged(view, newProgress);
            }

            // 设置程序的Title
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }
        });

        mWebView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK
                            && mWebView.canGoBack()) { // 表示按返回键

                        mWebView.goBack(); // 后退

                        // webview.goForward();//前进
                        return true; // 已处理
                    }
                }
                return false;
            }
        });
    }

    private void initWebSettings() {
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //设置适应屏幕
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        // 设置可以访问文件
        webSettings.setAllowFileAccess(true);
        webSettings.setNeedInitialFocus(true);
        webSettings.setDefaultTextEncodingName("UTF-8");
        // 设置可以支持缩放
        webSettings.setSupportZoom(true);
        //支持内容重新布局
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.supportMultipleWindows();
        webSettings.setSupportMultipleWindows(true);
        // 设置默认缩放方式尺寸是far
        webSettings.setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
        // 设置出现缩放工具
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDefaultFontSize(20);
        //设置缓存模式
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(mWebView.getContext().getCacheDir().getAbsolutePath());
        if (isNetworkAvailable(mContext)) {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else { webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);}
    }

    public boolean canBack() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return false;
        }
        return true;
    }

    /**
     * For security reasons, WebView is not allowed in privileged processes with 'android.uid.system'
     */
    private void hookWebView(){
        Log.i("CleanMaster", "call hookWebView ...");
        Class<?> factoryClass = null;
        try {
            factoryClass = Class.forName("android.webkit.WebViewFactory");
            Method getProviderClassMethod = null;
            Object sProviderInstance = null;

            Field field = factoryClass.getDeclaredField("sProviderInstance");
            field.setAccessible(true);
            if (null != field.get("sProviderInstance")) {
                Log.i("CleanMaster", "provider exist !");
                return;
            }

            if (Build.VERSION.SDK_INT <= 21) {//Android 21无WebView安全限制
                return;
            } else if (Build.VERSION.SDK_INT >= 23) {
                getProviderClassMethod = factoryClass.getDeclaredMethod("getProviderClass");
                getProviderClassMethod.setAccessible(true);
                Class<?> providerClass = (Class<?>) getProviderClassMethod.invoke(factoryClass);
                Class<?> delegateClass = Class.forName("android.webkit.WebViewDelegate");
                Constructor<?> constructor = providerClass.getConstructor(delegateClass);
                if (constructor != null) {
                    constructor.setAccessible(true);
                    Constructor<?> constructor2 = delegateClass.getDeclaredConstructor();
                    constructor2.setAccessible(true);
                    sProviderInstance = constructor.newInstance(constructor2.newInstance());
                }
            } else if (Build.VERSION.SDK_INT == 22) {
                getProviderClassMethod = factoryClass.getDeclaredMethod("getFactoryClass");
                getProviderClassMethod.setAccessible(true);
                Class<?> providerClass = (Class<?>) getProviderClassMethod.invoke(factoryClass);
                Class<?> delegateClass = Class.forName("android.webkit.WebViewDelegate");
                Constructor<?> constructor = providerClass.getConstructor(delegateClass);
                if (constructor != null) {
                    constructor.setAccessible(true);
                    Constructor<?> constructor2 = delegateClass.getDeclaredConstructor();
                    constructor2.setAccessible(true);
                    sProviderInstance = constructor.newInstance(constructor2.newInstance());
                }
            }/* else if (Build.VERSION.SDK_INT == 21) {//Android 21无WebView安全限制
                getProviderClassMethod = factoryClass.getDeclaredMethod("getFactoryClass");
                getProviderClassMethod.setAccessible(true);
                Class<?> providerClass = (Class<?>) getProviderClassMethod.invoke(factoryClass);
                sProviderInstance = providerClass.newInstance();
            }*/
            if (sProviderInstance != null) {
                Log.i("CleanMaster", "sProviderInstance=" + sProviderInstance.toString());
                //Field field = factoryClass.getDeclaredField("sProviderInstance");
                //field.setAccessible(true);
                field.set("sProviderInstance", sProviderInstance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isAvailable()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }
}
