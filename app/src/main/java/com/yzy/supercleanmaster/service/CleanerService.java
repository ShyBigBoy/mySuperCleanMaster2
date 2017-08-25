package com.yzy.supercleanmaster.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.yzy.supercleanmaster.R;
import com.yzy.supercleanmaster.bean.AppProcsInfo;
import com.yzy.supercleanmaster.bean.RunningProcInfo;
import com.yzy.supercleanmaster.model.CacheListItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.sleep;

public class CleanerService extends Service {

    public static final String ACTION_CLEAN_AND_EXIT = "com.tct.cleanerservice.CLEAN_AND_EXIT";
    public static final int IMPORTANCE_CANT_SAVE_STATE = 170;

    private static final String TAG = "CleanerService";

    private Method mGetPackageSizeInfoMethod, mFreeStorageAndNotifyMethod;
    private OnActionListener mOnActionListener;
    private boolean mIsScanning = false;
    private boolean mIsCleaning = false;
    private long mCacheSize = 0;

    ArrayMap<String, AppProcsInfo> m3rdAppProcsMap =null;
    ActivityManager activityManager = null;
    PackageManager packageManager = null;
    Context mContext;

    public interface OnActionListener {
        void onScanStarted(Context context);

        void onScanProgressUpdated(Context context, int current, int max);

        void onScanCompleted(Context context, List<CacheListItem> apps);

        void onCleanStarted(Context context);

        void onCleanCompleted(Context context, long cacheSize);
    }

    public class CleanerServiceBinder extends Binder {

        public CleanerService getService() {
            return CleanerService.this;
        }
    }

    private CleanerServiceBinder mBinder = new CleanerServiceBinder();

    private class TaskScan extends AsyncTask<Void, Integer, List<CacheListItem>> {

        private int mAppCount = 0;

        @Override
        protected void onPreExecute() {
            if (mOnActionListener != null) {
                mOnActionListener.onScanStarted(CleanerService.this);
            }
        }

        @Override
        protected List<CacheListItem> doInBackground(Void... params) {
            mCacheSize = 0;

            final List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(
                    PackageManager.GET_META_DATA);

            publishProgress(0, packages.size());

            final CountDownLatch countDownLatch = new CountDownLatch(packages.size());

            final List<CacheListItem> apps = new ArrayList<CacheListItem>();

            try {
                for (ApplicationInfo pkg : packages) {
                    mGetPackageSizeInfoMethod.invoke(getPackageManager(), pkg.packageName,
                            new IPackageStatsObserver.Stub() {

                                @Override
                                public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
                                        throws RemoteException {
                                    synchronized (apps) {
                                        publishProgress(++mAppCount, packages.size());

                                        if (succeeded && pStats.cacheSize > 0) {
                                            try {
                                                apps.add(new CacheListItem(pStats.packageName,
                                                        getPackageManager().getApplicationLabel(
                                                                getPackageManager().getApplicationInfo(
                                                                        pStats.packageName,
                                                                        PackageManager.GET_META_DATA)
                                                        ).toString(),
                                                        getPackageManager().getApplicationIcon(
                                                                pStats.packageName),
                                                        pStats.cacheSize
                                                ));

                                                mCacheSize += pStats.cacheSize;
                                            } catch (PackageManager.NameNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                    synchronized (countDownLatch) {
                                        countDownLatch.countDown();
                                    }
                                }
                            }
                    );
                }

                countDownLatch.await();
            } catch (InvocationTargetException | InterruptedException | IllegalAccessException e) {
                e.printStackTrace();
            }

            return new ArrayList<>(apps);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mOnActionListener != null) {
                mOnActionListener.onScanProgressUpdated(CleanerService.this, values[0], values[1]);
            }
        }

        @Override
        protected void onPostExecute(List<CacheListItem> result) {
            if (mOnActionListener != null) {
                mOnActionListener.onScanCompleted(CleanerService.this, result);
            }

            mIsScanning = false;
        }
    }

    private class TaskClean extends AsyncTask<Void, Void, Long> {

        @Override
        protected void onPreExecute() {
            if (mOnActionListener != null) {
                mOnActionListener.onCleanStarted(CleanerService.this);
            }
        }

        @Override
        protected Long doInBackground(Void... params) {
            final CountDownLatch countDownLatch = new CountDownLatch(1);

            StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());

            try {
                mFreeStorageAndNotifyMethod.invoke(getPackageManager(),
                        (long) stat.getBlockCount() * (long) stat.getBlockSize(),
                        new IPackageDataObserver.Stub() {
                            @Override
                            public void onRemoveCompleted(String packageName, boolean succeeded)
                                    throws RemoteException {
                                countDownLatch.countDown();
                            }
                        }
                );

                countDownLatch.await();
            } catch (InvocationTargetException | InterruptedException | IllegalAccessException e) {
                e.printStackTrace();
            }

            return mCacheSize;
        }

        @Override
        protected void onPostExecute(Long result) {
            mCacheSize = 0;

            if (mOnActionListener != null) {
                mOnActionListener.onCleanCompleted(CleanerService.this, result);
            }

            mIsCleaning = false;
        }
    }

    private class TaskCleanMemory extends AsyncTask<Void, Void, Long> {
        @Override
        protected void onPreExecute() {
            if (mOnActionListener != null) {
                mOnActionListener.onCleanStarted(CleanerService.this);
            }
        }

        @Override
        protected Long doInBackground(Void... params) {
            long totalMemoryCleaned = 0;
            try {
                sleep(1000);
                dumpRunningAppProcesses();
                totalMemoryCleaned = killBgProcsAssociatedWithPkgs();
            } catch (InterruptedException e) { e.printStackTrace();}
            return totalMemoryCleaned;
        }

        @Override
        protected void onPostExecute(Long aLong) {
            if (mOnActionListener != null) {
                mOnActionListener.onCleanCompleted(CleanerService.this, aLong);
            }
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "CleanerService.onBind called ...");
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "CleanerService.onCreate called ...");
        try {
            mGetPackageSizeInfoMethod = getPackageManager().getClass().getMethod(
                    "getPackageSizeInfo", String.class, IPackageStatsObserver.class);

            mFreeStorageAndNotifyMethod = getPackageManager().getClass().getMethod(
                    "freeStorageAndNotify", long.class, IPackageDataObserver.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        activityManager = (ActivityManager)
                getSystemService(Context.ACTIVITY_SERVICE);
        packageManager = getPackageManager();
        mContext = getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CleanerService.onStartCommand called ...");
        String action = intent.getAction();

        if (action != null) {
            if (action.equals(ACTION_CLEAN_AND_EXIT)) {
                Log.d(TAG, "com.tct.cleanerservice.CLEAN_AND_EXIT");
                setOnActionListener(new OnActionListener() {
                    @Override
                    public void onScanStarted(Context context) {}

                    @Override
                    public void onScanProgressUpdated(Context context, int current, int max) {}

                    @Override
                    public void onScanCompleted(Context context, List<CacheListItem> apps) {
                        if (getCacheSize() > 0) {
                            cleanCache();
                        }
                    }

                    @Override
                    public void onCleanStarted(Context context) {}

                    @Override
                    public void onCleanCompleted(Context context, long cacheSize) {
                        /*String msg = getString(R.string.cleaned, Formatter.formatShortFileSize(
                                CleanerService.this, cacheSize));
                        Log.d(TAG, msg);
                        Toast.makeText(CleanerService.this, msg, Toast.LENGTH_LONG).show();*/

                        Log.d(TAG, "Total clean memory size:" + cacheSize);
                        new Handler().postDelayed(()->stopSelf(), 5000);
                    }
                });

                //scanCache();
                cleanMemory();
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "CleanerService.onDestroy called ...");
        super.onDestroy();
    }

    public void scanCache() {
        mIsScanning = true;

        new TaskScan().execute();
    }

    public void cleanCache() {
        mIsCleaning = true;

        new TaskClean().execute();
    }

    public void cleanMemory() {
        new TaskCleanMemory().execute();
    }

    public void setOnActionListener(OnActionListener listener) {
        mOnActionListener = listener;
    }

    public boolean isScanning() {
        return mIsScanning;
    }

    public boolean isCleaning() {
        return mIsCleaning;
    }

    public long getCacheSize() {
        return mCacheSize;
    }

    protected void dumpRunningAppProcesses() {
        Log.d(TAG, "call dumpRunningAppProcesses ...");
        //packageManager = context.getPackageManager();
        //activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> appProcessList = activityManager.getRunningAppProcesses();
        Log.d(TAG, "Top App:" + appProcessList.get(0).pid + "-" + appProcessList.get(0).processName + "," + appProcessList.size());
        ApplicationInfo appInfo;
        m3rdAppProcsMap = new ArrayMap<>();
        RunningProcInfo abProcInfo = null;
        AppProcsInfo abAppProcsInfo = null;
        ArrayMap<String, RunningProcInfo> procMap = null;
        String appName;
        Drawable icon;
        String appFlag = "[null]";
        List<Integer> imeUisList = getInstalledIME();
        for (ActivityManager.RunningAppProcessInfo info : appProcessList) {
            if (imeUisList.contains(info.uid)) continue;
            try {
                appInfo = packageManager.getApplicationInfo(info.processName, 0);
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    Log.d(TAG, "3rd App ...");
                    appFlag = "[3rd]";
                    Log.d(TAG, "          " + info.uid + "-" +info.pid + "-" + info.processName + appFlag + importanceToString(info.importance));
                    Log.d(TAG, "               length:" + info.pkgList.length);

                    long memSize = activityManager.getProcessMemoryInfo(
                            new int[]{info.pid})[0].getTotalPrivateDirty() * 1024;
                    long averageMemory = memSize / info.pkgList.length;
                    abProcInfo = new RunningProcInfo(info, averageMemory);
                    for (int i =0; i < info.pkgList.length; ++i) {
                        Log.d(TAG, "               " + info.pkgList[i]);
                        if (info.processName.equals(info.pkgList[i])) { abProcInfo.isShareProcess = false; }
                        else { abProcInfo.isShareProcess = true; }
                        procMap = new ArrayMap<>();
                        procMap.put(info.processName, abProcInfo);
                        if (!m3rdAppProcsMap.containsKey(info.pkgList[i])) {
                            if (info.processName.equals(info.pkgList[i])) {
                                appName = appInfo.loadLabel(packageManager).toString();
                                icon = appInfo.loadIcon(packageManager);
                            } else {
                                try {
                                    appInfo = packageManager.getApplicationInfo(info.pkgList[i], 0);
                                    appName = appInfo.loadLabel(packageManager).toString();
                                    icon = appInfo.loadIcon(packageManager);
                                } catch (PackageManager.NameNotFoundException e1) {
                                    Log.d(TAG, "             Not found AppInfo!!!");
                                    appName = info.processName;
                                    icon = mContext.getResources().getDrawable(R.drawable.ic_launcher);
                                }
                            }
                            abAppProcsInfo = new AppProcsInfo(info.pkgList[i], appName, icon, info.uid, procMap);
                            abAppProcsInfo.processCount++;
                            abAppProcsInfo.memory += averageMemory;
                            if (info.importance < abAppProcsInfo.importance) { abAppProcsInfo.importance = info.importance;}
                            m3rdAppProcsMap.put(info.pkgList[i], abAppProcsInfo);
                        } else {
                            abAppProcsInfo = m3rdAppProcsMap.get(info.pkgList[i]);
                            abAppProcsInfo.processMap.put(info.processName, abProcInfo);
                            abAppProcsInfo.processCount ++;
                            abAppProcsInfo.memory += averageMemory;
                            if (info.importance < abAppProcsInfo.importance) { abAppProcsInfo.importance = info.importance;}
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException e2) {
                //e.printStackTrace();
                if (info.processName.indexOf(":") != -1) {//服务命名
                    //appInfo = getApplicationInfo2(info.processName.split(":")[0]);
                    try {
                        String servPkgName = info.processName.split(":")[0];
                        appInfo = packageManager.getApplicationInfo(servPkgName, 0);
                        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                            Log.d(TAG, "pkg+serviceName ...");
                            appFlag = "[3rd]";
                            Log.d(TAG, "          " + info.uid + "-" + info.pid + "-" + info.processName + appFlag + importanceToString(info.importance));
                            Log.d(TAG, "               length:" + info.pkgList.length);
                            long memSize = activityManager.getProcessMemoryInfo(
                                    new int[]{info.pid})[0].getTotalPrivateDirty() * 1024;
                            abProcInfo = new RunningProcInfo(info, memSize);
                            procMap = new ArrayMap<>();
                            procMap.put(info.processName, abProcInfo);
                            if (!m3rdAppProcsMap.containsKey(servPkgName)) {
                                appName = appInfo.loadLabel(packageManager).toString();
                                icon = appInfo.loadIcon(packageManager);
                                abAppProcsInfo = new AppProcsInfo(servPkgName, appName, icon, info.uid, procMap);
                                abAppProcsInfo.processCount++;
                                abAppProcsInfo.memory += memSize;
                                if (info.importance < abAppProcsInfo.importance) { abAppProcsInfo.importance = info.importance;}
                                m3rdAppProcsMap.put(servPkgName, abAppProcsInfo);
                            } else {
                                abAppProcsInfo = m3rdAppProcsMap.get(servPkgName);
                                abAppProcsInfo.processMap.put(info.processName, abProcInfo);
                                abAppProcsInfo.processCount ++;
                                abAppProcsInfo.memory += memSize;
                                if (info.importance < abAppProcsInfo.importance) { abAppProcsInfo.importance = info.importance;}
                            }

                        }
                    } catch (PackageManager.NameNotFoundException e3) {
                        Log.d(TAG, "invalid service ...");
                        Log.d(TAG, "                " + info.uid + "-" + info.pid + "-" + info.processName + importanceToString(info.importance));
                        Log.d(TAG, "               length:" + info.pkgList.length);
                        for (int i =0; i < info.pkgList.length; ++i) { Log.d(TAG, "               " + info.pkgList[i]); }
                    }
                } else {//customized process name
                    //appInfo = getApplicationInfo2(info.pkgList[0]);
                    try {
                        appInfo = packageManager.getApplicationInfo(info.pkgList[0], 0);
                        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                            Log.d(TAG, "custom process name ...");
                            appFlag = "[3rd]";
                            Log.d(TAG, "                " + info.uid + "-" + info.pid + "-" + info.processName + appFlag + importanceToString(info.importance));
                            Log.d(TAG, "                length:" + info.pkgList.length);
                            long memSize = activityManager.getProcessMemoryInfo(
                                    new int[]{info.pid})[0].getTotalPrivateDirty() * 1024;
                            abProcInfo = new RunningProcInfo(info, memSize);
                            procMap = new ArrayMap<>();
                            procMap.put(info.processName, abProcInfo);
                            if (!m3rdAppProcsMap.containsKey(info.pkgList[0])) {
                                appName = appInfo.loadLabel(packageManager).toString();
                                icon = appInfo.loadIcon(packageManager);
                                abAppProcsInfo = new AppProcsInfo(info.pkgList[0], appName, icon, info.uid, procMap);
                                abAppProcsInfo.processCount++;
                                abAppProcsInfo.memory += memSize;
                                if (info.importance < abAppProcsInfo.importance) { abAppProcsInfo.importance = info.importance;}
                                m3rdAppProcsMap.put(info.pkgList[0], abAppProcsInfo);
                            } else {
                                abAppProcsInfo = m3rdAppProcsMap.get(info.pkgList[0]);
                                abAppProcsInfo.processMap.put(info.processName, abProcInfo);
                                abAppProcsInfo.processCount ++;
                                abAppProcsInfo.memory += memSize;
                                if (info.importance < abAppProcsInfo.importance) { abAppProcsInfo.importance = info.importance;}
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e4) {
                        Log.d(TAG, "             Not found AppInfo!!!");
                        Log.d(TAG, "                " + info.uid + "-" + info.pid + "-" + info.processName + importanceToString(info.importance));
                        Log.d(TAG, "                length:" + info.pkgList.length);
                        for (int i =0; i < info.pkgList.length; ++i) { Log.d(TAG, "                " + info.pkgList[i]); }
                    }
                }
            }
        }
    }

    private List<Integer> getInstalledIME() {
        List<Integer> imeUidList = new ArrayList<>();
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> imiList = imm.getInputMethodList();
        for (InputMethodInfo imi : imiList) {
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(imi.getPackageName(), 0);
                imeUidList.add(appInfo.uid);
            } catch (PackageManager.NameNotFoundException e) {}

        }
        return imeUidList;
    }

    private ApplicationInfo getApplicationInfo2( String processName) {
        if (processName == null) {
            return null;
        }
        List<ApplicationInfo> appList = packageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (ApplicationInfo appInfo : appList) {
            if (processName.equals(appInfo.processName)) {
                return appInfo;
            }
        }
        return null;
    }

    private String importanceToString(int imp) {
        switch (imp) {
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND: return "[fg]";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE: return "[fgserv]";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING: return "[top-slp]";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE: return "[v]";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE: return "[per]";
            case IMPORTANCE_CANT_SAVE_STATE: return "[css]";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE: return "[serv]";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND: return "[bg]";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY: return "[emp]";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE: return "[go]";
            default: return "[non]";
        }
    }

    protected long killBgProcsAssociatedWithPkgs() {
        Log.d(TAG, "========================>Before clean:");
        long totalMemoryCleaned = 0;
        Iterator iter = m3rdAppProcsMap.entrySet().iterator();
        while (iter.hasNext()) {
            ArrayMap.Entry entry = (ArrayMap.Entry) iter.next();
            AppProcsInfo appProcsInfo = (AppProcsInfo) entry.getValue();
            Log.d(TAG, appProcsInfo.pkgName + "," + appProcsInfo.appName + "-" + appProcsInfo.processCount
                    + "," + appProcsInfo.uid + "," + appProcsInfo.memory + importanceToString(appProcsInfo.importance));
            Iterator procIter = appProcsInfo.processMap.values().iterator();
            while (procIter.hasNext()) {
                RunningProcInfo runningProcInfo = (RunningProcInfo) procIter.next();
                Log.d(TAG, "    " + runningProcInfo.processInfo.pid + "-" + runningProcInfo.processInfo.processName
                        + "," + runningProcInfo.isShareProcess + "," + runningProcInfo.memory);
            }
            if (appProcsInfo.importance <= IMPORTANCE_CANT_SAVE_STATE) continue;

            activityManager.killBackgroundProcesses((String) entry.getKey());
            //activityManager.forceStopPackage(m3rdAppProcsList.keyAt(0));

            try {
                Method forceStopPackage = activityManager.getClass()
                        .getDeclaredMethod("forceStopPackage", String.class);
                forceStopPackage.setAccessible(true);
                forceStopPackage.invoke(activityManager, (String) entry.getKey());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            totalMemoryCleaned += appProcsInfo.memory;
            iter.remove();
        }
        Log.d(TAG, "========================>After clean:");
        for(AppProcsInfo app : m3rdAppProcsMap.values()) {
            Log.d(TAG, app.pkgName + "," + app.appName + "-" + app.processCount
                    + "," + app.uid + "," + app.memory + importanceToString(app.importance));
        }

        m3rdAppProcsMap.clear();
        return totalMemoryCleaned;
    }
}
