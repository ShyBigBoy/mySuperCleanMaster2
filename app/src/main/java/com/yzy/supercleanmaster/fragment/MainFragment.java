package com.yzy.supercleanmaster.fragment;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.umeng.update.UmengUpdateAgent;
import com.yzy.supercleanmaster.R;
import com.yzy.supercleanmaster.base.BaseFragment;
import com.yzy.supercleanmaster.model.SDCardInfo;
import com.yzy.supercleanmaster.ui.AutoStartManageActivity;
import com.yzy.supercleanmaster.ui.MemoryCleanActivity;
import com.yzy.supercleanmaster.ui.RubbishCleanActivity;
import com.yzy.supercleanmaster.ui.SoftwareManageActivity;
import com.yzy.supercleanmaster.utils.AppUtil;
import com.yzy.supercleanmaster.utils.StorageUtil;
import com.yzy.supercleanmaster.widget.circleprogress.ArcProgress;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainFragment extends BaseFragment {
    /**
     * ViewPager+Fragment,when fragment >= 3,using 'ButterKnife' will cause a null object reference
     * It is recommended to use 'View.findViewById'
     */
    //@InjectView(R.id.arc_store)
    ArcProgress arcStore;

    //@InjectView(R.id.arc_process)
    ArcProgress arcProcess;
    //@InjectView(R.id.capacity)
    TextView capacity;

    Context mContext;
    private ActivityManager activityManager = null;

    private Timer timer;
    private Timer timer2;
    //avoid creating multiple instances of specified activity due to user's misoperation
    private boolean mSingleton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Override
    protected View initViews(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    //public View onCreateView(LayoutInflater inflater,
    //                         @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO Auto-generated method stub

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        //ButterKnife.inject(this, view);
        arcStore = (ArcProgress) view.findViewById(R.id.arc_store);
        arcProcess = (ArcProgress) view.findViewById(R.id.arc_process);
        capacity = (TextView) view.findViewById(R.id.capacity);
        view.findViewById(R.id.card1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speedUp();
            }
        });
        view.findViewById(R.id.card2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rubbishClean();
            }
        });
        view.findViewById(R.id.card3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AutoStartManage();
            }
        });
        view.findViewById(R.id.card4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoftwareManage();
            }
        });

        Log.i("CleanMaster", "MainFragment.initViews()");

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("CleanMaster", "MainFragment.onResume");
        fillData();
        mSingleton = false;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
        UmengUpdateAgent.update(getActivity());
        Log.i("CleanMaster", "MainFragment.onActivityCreated ");
    }

    @Override
    protected void initData() {
        Log.i("CleanMaster", "MainFragment.initData called");
    }

    @Override
    public boolean getUserVisibleHint() {
        Log.i("CleanMaster", "MainFragment.getUserVisibleHint() called");
        return super.getUserVisibleHint();
    }

    private void fillData() {
        Log.i("CleanMaster", "MainFragment.fillData called");
        // TODO Auto-generated method stub
        timer = null;
        timer2 = null;
        timer = new Timer();
        timer2 = new Timer();


        long l = AppUtil.getAvailMemory(mContext);
        long y = AppUtil.getTotalMemory(mContext);
        final double x = (((y - l) / (double) y) * 100);
        //   arcProcess.setProgress((int) x);
        //Log.i("CleanMaster","totalMem2=" + StorageUtil.convertStorage(y)
        //        + ",usedMem2=" + StorageUtil.convertStorage(y-l));

        arcProcess.setProgress(0);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Activity instance = getActivity();
                if (null != instance) {
                    instance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (arcProcess.getProgress() >= (int) x) {
                                timer.cancel();
                            } else {
                                arcProcess.setProgress(arcProcess.getProgress() + 1);
                            }

                        }
                    });
                }
            }
        }, 50, 20);

        SDCardInfo mSDCardInfo = StorageUtil.getSDCardInfo();
        SDCardInfo mSystemInfo = StorageUtil.getSystemSpaceInfo(mContext);

        long nAvailaBlock;
        long TotalBlocks;
        if (mSDCardInfo != null) {
            nAvailaBlock = mSDCardInfo.free + mSystemInfo.free;
            TotalBlocks = mSDCardInfo.total + mSystemInfo.total;
        } else {
            nAvailaBlock = mSystemInfo.free;
            TotalBlocks = mSystemInfo.total;
        }

        final double percentStore = (((TotalBlocks - nAvailaBlock) / (double) TotalBlocks) * 100);

        capacity.setText(StorageUtil.convertStorage(TotalBlocks - nAvailaBlock) + "/" + StorageUtil.convertStorage(TotalBlocks));
        arcStore.setProgress(0);

        timer2.schedule(new TimerTask() {
            @Override
            public void run() {
                Activity instance = getActivity();
                if (null != instance) {
                    instance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (arcStore.getProgress() >= (int) percentStore) {
                                timer2.cancel();
                            } else {
                                arcStore.setProgress(arcStore.getProgress() + 1);
                            }

                        }
                    });
                }
            }
        }, 50, 20);


    }

    //@OnClick(R.id.card1)
    void speedUp() {
        Log.i("CleanMaster", "MainFragment.speedUp111");
        if (!mSingleton) {
            startActivity(MemoryCleanActivity.class);
            mSingleton = true;
            Log.i("CleanMaster", "MainFragment.speedUp222");
        }
    }


    //@OnClick(R.id.card2)
    void rubbishClean() {
        if (!mSingleton) {
            startActivity(RubbishCleanActivity.class);
            mSingleton = true;
        }
    }


    //@OnClick(R.id.card3)
    void AutoStartManage() {
        Log.i("CleanMaster", "MainFragment.AutoStartManage111");
        if (!mSingleton) {
            startActivity(AutoStartManageActivity.class);
            mSingleton = true;
            Log.i("CleanMaster", "MainFragment.AutoStartManage222");
        }
    }

    //@OnClick(R.id.card4)
    void SoftwareManage() {
        if (!mSingleton) {
            startActivity(SoftwareManageActivity.class);
            mSingleton = true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //ButterKnife.reset(this);
        Log.i("CleanMaster", "MainFragment.onDestroyView");
    }


    @Override
    public void onDestroy() {
        Log.i("CleanMaster", "MainFragment.onDestroy");
        timer.cancel();
        timer2.cancel();
        super.onDestroy();
    }
}
