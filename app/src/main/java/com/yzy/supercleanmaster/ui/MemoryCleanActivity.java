package com.yzy.supercleanmaster.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.ArrayMap;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.etiennelawlor.quickreturn.library.enums.QuickReturnType;
import com.etiennelawlor.quickreturn.library.listeners.QuickReturnListViewOnScrollListener;
import com.john.waveview.WaveView;
import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;
import com.yzy.supercleanmaster.R;
import com.yzy.supercleanmaster.adapter.ClearMemoryAdapter;
import com.yzy.supercleanmaster.adapter.ClearMemoryAdapter2;
import com.yzy.supercleanmaster.base.BaseSwipeBackActivity;
import com.yzy.supercleanmaster.bean.AppProcessInfo;
import com.yzy.supercleanmaster.bean.AppProcsInfo;
import com.yzy.supercleanmaster.model.StorageSize;
import com.yzy.supercleanmaster.service.CoreService;
import com.yzy.supercleanmaster.utils.StorageUtil;
import com.yzy.supercleanmaster.utils.SystemBarTintManager;
import com.yzy.supercleanmaster.utils.T;
import com.yzy.supercleanmaster.utils.UIElementsHelper;
//import com.yzy.supercleanmaster.widget.textcounter.CounterView;
//import com.yzy.supercleanmaster.widget.textcounter.formatters.DecimalFormatter;
import com.github.premnirmal.textcounter.CounterView;
import com.github.premnirmal.textcounter.formatters.DecimalFormatter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import butterknife.InjectView;
import butterknife.OnClick;


public class MemoryCleanActivity extends BaseSwipeBackActivity implements OnDismissCallback,
        CoreService.OnPeocessActionListener, ClearMemoryAdapter2.OnItemCheckedListener {

    ActionBar ab;

    @InjectView(R.id.listview)
    ListView mListView;

    @InjectView(R.id.wave_view)
    WaveView mwaveView;


    @InjectView(R.id.header)
    RelativeLayout header;
    List<AppProcessInfo> mAppProcessInfos = new ArrayList<>();
    ArrayMap<String, AppProcsInfo> mAppProcsInfos = new ArrayMap<>();
    ClearMemoryAdapter mClearMemoryAdapter;
    ClearMemoryAdapter2 mClearMemoryAdapter2;

    @InjectView(R.id.textCounter)
    CounterView textCounter;
    @InjectView(R.id.sufix)
    TextView sufix;
    @InjectView(R.id.detail)
    TextView tvDetail;
    public long Allmemory;
    private long usedMemory;

    @InjectView(R.id.bottom_lin)
    LinearLayout bottom_lin;

    @InjectView(R.id.progressBar)
    View mProgressBar;
    @InjectView(R.id.progressBarText)
    TextView mProgressBarText;

    @InjectView(R.id.clear_button)
    Button clearButton;
    private static final int INITIAL_DELAY_MILLIS = 300;
    SwingBottomInAnimationAdapter swingBottomInAnimationAdapter;

    private CoreService mCoreService;
    ActivityManager activityManager = null;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCoreService = ((CoreService.ProcessServiceBinder) service).getService();
            mCoreService.setOnActionListener(MemoryCleanActivity.this);
            //mCoreService.scanRunProcess();
            mCoreService.scanMemory();
            //  updateStorageUsage();


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCoreService.setOnActionListener(null);
            mCoreService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_clean);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        //  applyKitKatTranslucency();
        //mClearMemoryAdapter = new ClearMemoryAdapter(mContext, mAppProcessInfos);
        mClearMemoryAdapter2 = new ClearMemoryAdapter2(mContext, mAppProcsInfos);
        mListView.setAdapter(mClearMemoryAdapter2);
        mClearMemoryAdapter2.setOnItemCheckedListener(this);
        bindService(new Intent(mContext, CoreService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE);
        int footerHeight = mContext.getResources().getDimensionPixelSize(R.dimen.footer_height);
        mListView.setOnScrollListener(new QuickReturnListViewOnScrollListener(QuickReturnType.FOOTER, null, 0, bottom_lin, footerHeight));

        textCounter.setAutoFormat(false);
        textCounter.setFormatter(new DecimalFormatter());
        textCounter.setAutoStart(false);
        textCounter.setIncrement(5f); // the amount the number increments at each time interval
        textCounter.setTimeInterval(50); // the time interval (ms) at which the text changes
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Apply KitKat specific translucency.
     */
    private void applyKitKatTranslucency() {

        // KitKat translucent navigation/status bar.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setTranslucentStatus(true);
            SystemBarTintManager mTintManager = new SystemBarTintManager(this);
            mTintManager.setStatusBarTintEnabled(true);
            mTintManager.setNavigationBarTintEnabled(true);
            // mTintManager.setTintColor(0xF00099CC);

            mTintManager.setTintDrawable(UIElementsHelper
                    .getGeneralActionBarBackground(this));

            getActionBar().setBackgroundDrawable(
                    UIElementsHelper.getGeneralActionBarBackground(this));

        }

    }

    @TargetApi(19)
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    @Override
    public void onDismiss(@NonNull ViewGroup viewGroup, @NonNull int[] ints) {

    }

    @Override
    public void onScanStarted(Context context) {
        mProgressBarText.setText(R.string.scanning);
        showProgressBar(true);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        usedMemory = memoryInfo.totalMem - memoryInfo.availMem;
    }

    @Override
    public void onScanProgressUpdated(Context context, int current, int max) {
        mProgressBarText.setText(getString(R.string.scanning_m_of_n, current, max));
    }

    @Override
    public void onScanCompleted(Context context, List<AppProcessInfo> apps) {
        mAppProcessInfos.clear();

        Allmemory = 0;
        for (AppProcessInfo appInfo : apps) {
            //if (!appInfo.isSystem && !"com.yzy.supercleanmaster".equals(appInfo.processName)) {
            if (!"com.yzy.supercleanmaster".equals(appInfo.processName)) {
                mAppProcessInfos.add(appInfo);
                Allmemory += appInfo.memory;
            }
        }

        refeshTextCounter();

        mClearMemoryAdapter.notifyDataSetChanged();
        showProgressBar(false);

        if (apps.size() > 0) {
            header.setVisibility(View.VISIBLE);
            bottom_lin.setVisibility(View.VISIBLE);
        } else {
            header.setVisibility(View.GONE);
            bottom_lin.setVisibility(View.GONE);
        }
//        mClearMemoryAdapter = new ClearMemoryAdapter(mContext,
//                apps);  mClearMemoryAdapter = new ClearMemoryAdapter(mContext,
//                apps);
//        swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(new SwipeDismissAdapter(mClearMemoryAdapter, MemoryCleanActivity.this));
//        swingBottomInAnimationAdapter.setAbsListView(mListView);
//        assert swingBottomInAnimationAdapter.getViewAnimator() != null;
//        swingBottomInAnimationAdapter.getViewAnimator().setInitialDelayMillis(INITIAL_DELAY_MILLIS);
//
//        mListView.setAdapter(swingBottomInAnimationAdapter);
        //clearMem.setText("200M");
    }

    @Override
    public void onScanCompleted2(ArrayMap<String, AppProcsInfo> apps) {
        mAppProcsInfos.clear();
        Allmemory = 0;

        mAppProcsInfos.putAll(apps);
        Iterator iter = apps.entrySet().iterator();
        while (iter.hasNext()) {
            AppProcsInfo appProcsInfo = (AppProcsInfo) ((ArrayMap.Entry) iter.next()).getValue();
            if (appProcsInfo.checked) { Allmemory += appProcsInfo.memory;}
        }

        refeshTextCounter();

        mClearMemoryAdapter2.notifyDataSetChanged();
        showProgressBar(false);

        if (apps.size() > 0) {
            header.setVisibility(View.VISIBLE);
            bottom_lin.setVisibility(View.VISIBLE);
        } else {
            header.setVisibility(View.GONE);
            bottom_lin.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCleanStarted(Context context) {}

    @Override
    public void onCleanCompleted(Context context, long cacheSize) {
        mAppProcsInfos.clear();
        mClearMemoryAdapter2.notifyDataSetChanged();
        Allmemory = cacheSize;
        tvDetail.setText("内存已释放");
        refeshTextCounter();
        bottom_lin.setVisibility(View.GONE);
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(1);
        T.showLong(mContext, "共清理" + StorageUtil.convertStorage(cacheSize) + "内存,已加速"
                + nf.format((float)cacheSize/usedMemory));
    }

    @Override
    public void onItemChecked(View v, boolean isChecked, long memory) {
        refeshTextCounter(isChecked, memory);
    }

    @OnClick(R.id.clear_button)
    public void onClickClear() {
        /*long killAppmemory = 0;
        for (int i = mAppProcessInfos.size() - 1; i >= 0; i--) {
            if (mAppProcessInfos.get(i).checked) {
                killAppmemory += mAppProcessInfos.get(i).memory;
                mCoreService.killBackgroundProcesses(mAppProcessInfos.get(i).processName);
                mAppProcessInfos.remove(mAppProcessInfos.get(i));
                mClearMemoryAdapter.notifyDataSetChanged();
            }
        }
        Allmemory = Allmemory - killAppmemory;
        T.showLong(mContext, "共清理" + StorageUtil.convertStorage(killAppmemory) + "内存");
        if (Allmemory > 0) {
            refeshTextCounter();
        }*/

        mCoreService.cleanMemory(true);
    }

    private void refeshTextCounter() {
        mwaveView.setProgress(20);
        StorageSize mStorageSize = StorageUtil.convertStorageSize(Allmemory);
        textCounter.setStartValue(0f);
        textCounter.setEndValue(mStorageSize.value);
        sufix.setText(mStorageSize.suffix);
        //  textCounter.setSuffix(mStorageSize.suffix);
        textCounter.start();
    }

    private void refeshTextCounter(boolean isChecked, long memory) {
        mwaveView.setProgress(20);
        StorageSize mStorageSize = StorageUtil.convertStorageSize(Allmemory);
        textCounter.setStartValue(mStorageSize.value);
        Allmemory = isChecked ? (Allmemory + memory) : (Allmemory - memory);
        mStorageSize = StorageUtil.convertStorageSize(Allmemory);
        textCounter.setEndValue(mStorageSize.value);
        sufix.setText(mStorageSize.suffix);
        //  textCounter.setSuffix(mStorageSize.suffix);
        textCounter.start();
    }

    private void showProgressBar(boolean show) {
        if (show) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.startAnimation(AnimationUtils.loadAnimation(
                    mContext, android.R.anim.fade_out));
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        unbindService(mServiceConnection);
        super.onDestroy();
    }
}
