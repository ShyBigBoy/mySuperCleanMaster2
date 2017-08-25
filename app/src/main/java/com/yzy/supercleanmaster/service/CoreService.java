package com.yzy.supercleanmaster.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import android.content.BroadcastReceiver;
import android.util.ArrayMap;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.yzy.supercleanmaster.R;
import com.yzy.supercleanmaster.bean.AppProcessInfo;
import com.yzy.supercleanmaster.bean.AppProcsInfo;
import com.yzy.supercleanmaster.bean.RunningProcInfo;

import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import static java.lang.Thread.sleep;


public class CoreService extends Service {

    public static final String ACTION_CLEAN_AND_EXIT = "com.tct.coreservice.CLEAN_AND_EXIT";

    public static final String TAG = "CleanerService";
    public static final String ACTION_AUTO_CLEAN = "com.tct.autoclean.4test1";
    public static final String ACTION_CLEAN_BG_PROCESSES = "com.tct.coreservice.CLEAN_BG_PROCESSES";
    public static final int IMPORTANCE_CANT_SAVE_STATE = 170;


    private OnPeocessActionListener mOnActionListener;
    private boolean mIsScanning = false;
    private boolean mIsCleaning = false;

    ActivityManager activityManager = null;
    UsageStatsManager mUsageStatsManager = null;
    List<AppProcessInfo> list = null;
    PackageManager packageManager = null;
    NotificationManager notificationManager = null;
    Context mContext;
    ArrayMap<String, AppProcsInfo> m3rdAppProcsMap =null;
    AutoCleanReceiver autoCleanReceiver = null;
    Notification.Builder mBuilder = null;

    public interface OnPeocessActionListener {
        void onScanStarted(Context context);
        void onScanProgressUpdated(Context context, int current, int max);
        void onScanCompleted(Context context, List<AppProcessInfo> apps);
        void onScanCompleted2(ArrayMap<String, AppProcsInfo> apps);
        void onCleanStarted(Context context);
        void onCleanCompleted(Context context, long cacheSize);
    }

    public class ProcessServiceBinder extends Binder {

        public CoreService getService() {
            return CoreService.this;
        }
    }

    private ProcessServiceBinder mBinder = new ProcessServiceBinder();


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "CoreService.onBind called ...");
        return mBinder;
    }

    @Override
    public void onCreate() {
        mContext = getApplicationContext();

        autoCleanReceiver = new AutoCleanReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_AUTO_CLEAN);
        registerReceiver(autoCleanReceiver, filter);

        try {
            activityManager = (ActivityManager)
                    getSystemService(Context.ACTIVITY_SERVICE);
            packageManager = getPackageManager();
            if (mUsageStatsManager == null) {
                mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            }
        } catch (Exception e) { e.printStackTrace();}

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notifIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder = new Notification.Builder(this);
        Notification notification = mBuilder.setSmallIcon(R.drawable.small_icon)
                .setContentTitle("This is CoreService")
                .setContentText("Touch disabled").build();
                //.setContentIntent(pendingIntent).build();
        //startForeground(1, notification);
        notificationManager.notify(1, notification);

        Log.d(TAG, "CoreService.onCreate called ...");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "CoreService.onDestroy called ...");
        super.onDestroy();
        unregisterReceiver(autoCleanReceiver);
        //stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CoreService.onStartCommand called ...");
        String action = intent.getAction();
        if (action != null) {
            if (ACTION_CLEAN_AND_EXIT.equals(action)) {
                Log.d(TAG, "com.tct.coreservice.CLEAN_AND_EXIT");
                setOnActionListener(new OnPeocessActionListener() {
                    @Override
                    public void onScanStarted(Context context) {}

                    @Override
                    public void onScanProgressUpdated(Context context, int current, int max) {}

                    @Override
                    public void onScanCompleted(Context context, List<AppProcessInfo> apps) {
                        //   if (getCacheSize() > 0) {
                        //     cleanCache();
                        // }
                    }

                    @Override
                    public void onScanCompleted2(ArrayMap<String, AppProcsInfo> apps) {}

                    @Override
                    public void onCleanStarted(Context context) {}

                    @Override
                    public void onCleanCompleted(Context context, long cacheSize) {
                        String msg = getString(R.string.cleaned, Formatter.formatShortFileSize(
                                CoreService.this, cacheSize));
                        Log.d(TAG, msg);
                        Toast.makeText(CoreService.this, msg, Toast.LENGTH_LONG).show();

                        new Handler().postDelayed(()->stopSelf(), 5000);
                    }
                });

                scanRunProcess();
            } else if (ACTION_CLEAN_BG_PROCESSES.equals(action)) {
                Log.d(TAG, "com.tct.coreservice.CLEAN_BG_PROCESSES");
                setOnActionListener(new OnPeocessActionListener() {
                    @Override
                    public void onScanStarted(Context context) {}

                    @Override
                    public void onScanProgressUpdated(Context context, int current, int max) {}

                    @Override
                    public void onScanCompleted(Context context, List<AppProcessInfo> apps) {}

                    @Override
                    public void onScanCompleted2(ArrayMap<String, AppProcsInfo> apps) {
                        cleanMemory(false);
                    }

                    @Override
                    public void onCleanStarted(Context context) {}

                    @Override
                    public void onCleanCompleted(Context context, long cacheSize) {
                        String msg = new String("During Lock-Screen,Release Memory：" + cacheSize/1024/1024 + " MB");
                        Log.d(TAG, msg);
                        Notification notification = mBuilder.setContentTitle(msg).build();
                        notificationManager.notify(1, notification);
                        //new Handler().postDelayed(()->stopSelf(), 5000);
                    }
                });
                scanMemory();
            }
        }

        return START_NOT_STICKY;
    }


    private class TaskScan extends AsyncTask<Void, Integer, List<AppProcessInfo>> {

        private int mAppCount = 0;

        @Override
        protected void onPreExecute() {
            if (mOnActionListener != null) {
                mOnActionListener.onScanStarted(CoreService.this);
            }
        }

        @Override
        protected List<AppProcessInfo> doInBackground(Void... params) {
            list = new ArrayList<AppProcessInfo>();
            ApplicationInfo appInfo = null;
            AppProcessInfo abAppProcessInfo = null;

            List<ActivityManager.RunningAppProcessInfo> appProcessList = activityManager.getRunningAppProcesses();
            //List<ActivityManager.RunningAppProcessInfo> appProcessList = getRunningAppList();

            publishProgress(0, appProcessList.size());

            for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessList) {
                publishProgress(++mAppCount, appProcessList.size());
                abAppProcessInfo = new AppProcessInfo(
                        appProcessInfo.processName, appProcessInfo.pid,
                        appProcessInfo.uid);
                try {
                    appInfo = packageManager.getApplicationInfo(appProcessInfo.processName, 0);


                    if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        abAppProcessInfo.isSystem = true;
                    } else {
                        abAppProcessInfo.isSystem = false;
                    }
                    Drawable icon = appInfo.loadIcon(packageManager);
                    String appName = appInfo.loadLabel(packageManager)
                            .toString();
                    abAppProcessInfo.icon = icon;
                    abAppProcessInfo.appName = appName;
                } catch (PackageManager.NameNotFoundException e) {
                    //   e.printStackTrace();

                    // :服务的命名

                    if (appProcessInfo.processName.indexOf(":") != -1) {
                        appInfo = getApplicationInfo(appProcessInfo.processName.split(":")[0]);
                        if (appInfo != null) {
                            Drawable icon = appInfo.loadIcon(packageManager);
                            abAppProcessInfo.icon = icon;
                        }else{
                            abAppProcessInfo.icon = mContext.getResources().getDrawable(R.drawable.ic_launcher);
                        }

                    }else{
                        abAppProcessInfo.icon = mContext.getResources().getDrawable(R.drawable.ic_launcher);
                    }
                    abAppProcessInfo.isSystem = true;
                    abAppProcessInfo.appName = appProcessInfo.processName;
                }


                long memsize = activityManager.getProcessMemoryInfo(new int[]{appProcessInfo.pid})[0].getTotalPrivateDirty() * 1024;
                abAppProcessInfo.memory = memsize;

                list.add(abAppProcessInfo);
            }


            return list;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mOnActionListener != null) {
                mOnActionListener.onScanProgressUpdated(CoreService.this, values[0], values[1]);
            }
        }

        @Override
        protected void onPostExecute(List<AppProcessInfo> result) {
            if (mOnActionListener != null) {
                mOnActionListener.onScanCompleted(CoreService.this, result);
            }

            mIsScanning = false;
        }
    }

    private class TaskClean extends AsyncTask<Void, Void, Long> {

        @Override
        protected void onPreExecute() {
            if (mOnActionListener != null) {
                mOnActionListener.onCleanStarted(CoreService.this);
            }
        }

        @Override
        protected Long doInBackground(Void... params) {
            long beforeMemory = 0;
            long endMemory = 0;
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            beforeMemory = memoryInfo.availMem;
            List<ActivityManager.RunningAppProcessInfo> appProcessList = activityManager
                    .getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo info : appProcessList) {
                killBackgroundProcesses(info.processName);
            }
            activityManager.getMemoryInfo(memoryInfo);
            endMemory = memoryInfo.availMem;
            return endMemory - beforeMemory;
        }

        @Override
        protected void onPostExecute(Long result) {


            if (mOnActionListener != null) {
                mOnActionListener.onCleanCompleted(CoreService.this, result);
            }


        }
    }

    private class TaskScanMemory extends AsyncTask<Void, Integer, ArrayMap<String, AppProcsInfo>> {
        @Override
        protected void onPreExecute() {
            if (mOnActionListener != null) {
                mOnActionListener.onScanStarted(CoreService.this);
            }
        }

        @Override
        protected ArrayMap<String, AppProcsInfo> doInBackground(Void... params) {
            //long totalMemoryCleaned = 0;
            try {
                sleep(2000);
                dumpRunningAppProcesses();
                //totalMemoryCleaned = killBgProcsAssociatedWithPkgs();
            } catch (InterruptedException e) { e.printStackTrace();}
            return m3rdAppProcsMap;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (null != mOnActionListener) {
                mOnActionListener.onScanProgressUpdated(CoreService.this, values[0], values[1]);
            }
        }

        @Override
        protected void onPostExecute(ArrayMap<String, AppProcsInfo> result) {
            if (mOnActionListener != null) {
                mOnActionListener.onScanCompleted2(result);
            }
        }

        protected void dumpRunningAppProcesses() {
            Log.d(TAG, "call dumpRunningAppProcesses ...");
            ApplicationInfo appInfo;
            m3rdAppProcsMap = new ArrayMap<>();
            RunningProcInfo abProcInfo = null;
            AppProcsInfo abAppProcsInfo = null;
            ArrayMap<String, RunningProcInfo> procMap = null;
            String appName;
            Drawable icon;
            String appFlag = "[null]";
            int procCount = 0;
            final List<RunningAppProcessInfo> appProcessList = activityManager.getRunningAppProcesses();
            int totalProcCount = appProcessList.size();
            Log.d(TAG, "Top App:" + appProcessList.get(0).pid + "-" + appProcessList.get(0).processName + "," + totalProcCount);
            publishProgress(0, totalProcCount);
            List<Integer> imeUisList = getInstalledIME();
            for (RunningAppProcessInfo info : appProcessList) {
                publishProgress(++procCount, totalProcCount);
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
            Log.d(TAG, "========================>Apps list before clean:");
            Iterator iter = m3rdAppProcsMap.entrySet().iterator();
            while (iter.hasNext()) {
                ArrayMap.Entry entry = (ArrayMap.Entry) iter.next();
                AppProcsInfo appProcsInfo = (AppProcsInfo) entry.getValue();
                Log.d(TAG, "    " + appProcsInfo.pkgName + "," + appProcsInfo.appName + "-" + appProcsInfo.processCount
                        + "," + appProcsInfo.uid + "," + appProcsInfo.memory + importanceToString(appProcsInfo.importance));
                Iterator procIter = appProcsInfo.processMap.values().iterator();
                while (procIter.hasNext()) {
                    RunningProcInfo runningProcInfo = (RunningProcInfo) procIter.next();
                    Log.d(TAG, "        " + runningProcInfo.processInfo.pid + "-" + runningProcInfo.processInfo.processName
                            + "," + runningProcInfo.isShareProcess + "," + runningProcInfo.memory);
                }
            }
        }
    }

    private class TaskCleanMemory extends AsyncTask<Boolean, Void, Long> {
        @Override
        protected void onPreExecute() {
            if (mOnActionListener != null) {
                mOnActionListener.onCleanStarted(CoreService.this);
            }
        }

        @Override
        protected Long doInBackground(Boolean... params) {
            long totalMemoryCleaned = 0;
            totalMemoryCleaned = killBgProcsAssociatedWithPkgs(params[0]);
            return totalMemoryCleaned;
        }

        @Override
        protected void onPostExecute(Long aLong) {
            if (mOnActionListener != null) {
                mOnActionListener.onCleanCompleted(CoreService.this, aLong);
            }
        }
    }

    public long getAvailMemory(Context context) {
        // 获取android当前可用内存大小
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        // 当前系统可用内存 ,将获得的内存大小规格化

        return memoryInfo.availMem;
    }

    public void cleanAllProcess() {
        //  mIsCleaning = true;

        new TaskClean().execute();
    }

    public void setOnActionListener(OnPeocessActionListener listener) {
        mOnActionListener = listener;
    }

    public ApplicationInfo getApplicationInfo( String processName) {
        if (processName == null) {
            return null;
        }
        List<ApplicationInfo> appList = packageManager
                .getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (ApplicationInfo appInfo : appList) {
            if (processName.equals(appInfo.processName)) {
                return appInfo;
            }
        }
        return null;
    }

    public boolean isScanning() {
        return mIsScanning;
    }

    public boolean isCleaning() {
        return mIsCleaning;
    }

    public void killBackgroundProcesses(String processName) {
        // mIsScanning = true;

        String packageName = null;
        try {
            if (processName.indexOf(":") == -1) {
                packageName = processName;
            } else {
                packageName = processName.split(":")[0];
            }

            activityManager.killBackgroundProcesses(packageName);

            //
            Method forceStopPackage = activityManager.getClass()
                    .getDeclaredMethod("forceStopPackage", String.class);
            forceStopPackage.setAccessible(true);
            forceStopPackage.invoke(activityManager, packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public List<ActivityManager.RunningAppProcessInfo> getRunningAppList() {
        List<ActivityManager.RunningAppProcessInfo> appProcessList = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //Android 5.0+ killed getRunningTasks(int) and getRunningAppProcesses(). Both of those methods are now
            //deprecated and only return your application process
            appProcessList = activityManager.getRunningAppProcesses();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            //Google has significantly restricted access to /proc in Android Nougat. This library will not work on Android 7.0
            appProcessList = new ArrayList<>();
            List<AndroidAppProcess> runningAppProcesses = AndroidProcesses.getRunningAppProcesses();
            for (AndroidAppProcess process : runningAppProcesses) {
                ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo(process.name, process.pid, null);
                info.uid = process.uid;
                // TODO: Get more information about the process. pkgList, importance, lru, etc.
                appProcessList.add(info);
            }
        }
        return appProcessList;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public List<String> getRecentUsedApps(int during) {
        List<String> recentUsed = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        long startTime = calendar.getTimeInMillis();
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(startTime, endTime);
        if (null != usageEvents) {
            while (usageEvents.hasNextEvent()) {
                UsageEvents.Event curEvent = new UsageEvents.Event();
                usageEvents.getNextEvent(curEvent);
                if ((endTime - curEvent.getTimeStamp() <= during * 60 * 1000)
                        && (UsageEvents.Event.USER_INTERACTION == curEvent.getEventType())) {
                    String pkgName = curEvent.getPackageName();
                    if (!recentUsed.contains(pkgName)) { recentUsed.add(pkgName);}
                }
            }
        }

        /*List<UsageStats> usageStatsList = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        if ((null != usageStatsList) && !usageStatsList.isEmpty()) {
            for (UsageStats usageStats : usageStatsList) {
                if (endTime - usageStats.getLastTimeUsed() >= during*60*1000) {
                    recentUnused.add(usageStats.getPackageName());
                }
            }
        }*/
        return recentUsed;
    }



    public void killBGProcesses(String pkgName) {
        try {
            activityManager.killBackgroundProcesses(pkgName);
            //activityManager.forceStopPackage(pkgName);

            Method forceStopPackage = activityManager.getClass()
                    .getDeclaredMethod("forceStopPackage", String.class);
            forceStopPackage.setAccessible(true);
            forceStopPackage.invoke(activityManager, pkgName);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public String getFGApp() {
        List<RunningTaskInfo> task = activityManager.getRunningTasks(1);
        if (!task.isEmpty() && null != task) {
            String pkgName = task.get(0).topActivity.getPackageName();
            Log.d(TAG, "FG App:" + pkgName);
            return pkgName;
        }
        return null;
    }

    private List<Integer> getInstalledIME() {
        List<Integer> imeUidList = new ArrayList<>();
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> imiList = imm.getInputMethodList();
        if (!imiList.isEmpty() && null != imiList) {
            for (InputMethodInfo imi : imiList) {
                try {
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(imi.getPackageName(), 0);
                    Log.d(TAG, "    IME:" + imi.getPackageName());
                    imeUidList.add(appInfo.uid);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

            }
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
            case RunningAppProcessInfo.IMPORTANCE_FOREGROUND: return "[fg]";
            case RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE: return "[fgserv]";
            case RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING: return "[top-slp]";
            case RunningAppProcessInfo.IMPORTANCE_VISIBLE: return "[v]";
            case RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE: return "[per]";
            case IMPORTANCE_CANT_SAVE_STATE: return "[css]";
            case RunningAppProcessInfo.IMPORTANCE_SERVICE: return "[serv]";
            case RunningAppProcessInfo.IMPORTANCE_BACKGROUND: return "[bg]";
            case RunningAppProcessInfo.IMPORTANCE_EMPTY: return "[emp]";
            case RunningAppProcessInfo.IMPORTANCE_GONE: return "[go]";
            default: return "[non]";
        }
    }

    protected long killBgProcsAssociatedWithPkgs(boolean isForeground) {
        long beforeMemory = 0, endMemory = 0;
        List<String> recent15MinsUsed = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            recent15MinsUsed = getRecentUsedApps(5);
            if (null != recent15MinsUsed && !recent15MinsUsed.isEmpty()) {
                Log.d(TAG, "========================>Apps used for last 5 Mins:");
                for (String str : recent15MinsUsed) { Log.d(TAG, "    " + str);}
            }
        }
        String fgApp = getFGApp();
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        beforeMemory = memoryInfo.availMem;

        long totalMemoryCleaned = 0;
        Iterator iter = m3rdAppProcsMap.entrySet().iterator();
        while (iter.hasNext()) {
            ArrayMap.Entry entry = (ArrayMap.Entry) iter.next();
            AppProcsInfo appProcsInfo = (AppProcsInfo) entry.getValue();

            if (isForeground) {
                if (!appProcsInfo.checked) continue;
            } else {
                if (appProcsInfo.importance <= IMPORTANCE_CANT_SAVE_STATE) {
                    if (appProcsInfo.importance != RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            && appProcsInfo.importance != RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING) {
                        continue;
                    } else {
                        //kill the processes who illegally promote their own priority to 'FOREGROUND'
                        if (appProcsInfo.pkgName.equals(fgApp)) {
                            continue;
                        }
                    }
                }

                if (null != recent15MinsUsed && !recent15MinsUsed.isEmpty()) {
                    if (recent15MinsUsed.contains(appProcsInfo.pkgName)) {
                        continue;
                    }
                }
            }

            killBGProcesses((String) entry.getKey());

            totalMemoryCleaned += appProcsInfo.memory;
            iter.remove();
        }
        Log.d(TAG, "========================>Apps list after clean:");
        activityManager.getMemoryInfo(memoryInfo);
        endMemory = memoryInfo.availMem;

        for(AppProcsInfo app : m3rdAppProcsMap.values()) {
            Log.d(TAG, "    " + app.pkgName + "," + app.appName + "-" + app.processCount
                    + "," + app.uid + "," + app.memory + importanceToString(app.importance));
        }

        Log.d(TAG, "totalMemoryCleaned size=" + totalMemoryCleaned + "," + (endMemory-beforeMemory));
        m3rdAppProcsMap.clear();
        return endMemory-beforeMemory;
    }

    public void scanRunProcess() {
        // mIsScanning = true;

        new TaskScan().execute();
    }

    public void scanMemory() { new TaskScanMemory().execute();}

    public void cleanMemory(boolean isForeground) { new TaskCleanMemory().execute(isForeground);}
    /**
     * This receiver catches when inputs *#*#666#*#* through any Dialer,
     * then activity 'com.android.settings.OperateGMSAPKActivity' will be launched soon.
     */
    private class AutoCleanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_AUTO_CLEAN.equals(intent.getAction())) {
                //dumpRunningAppProcesses();
                //killBgProcsAssociatedWithPkgs();

                context.startService(new Intent(CleanerService.ACTION_CLEAN_AND_EXIT, null, context, CleanerService.class));
            }
        }
    }

}
