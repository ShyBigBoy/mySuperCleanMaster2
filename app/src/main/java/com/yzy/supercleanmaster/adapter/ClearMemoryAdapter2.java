package com.yzy.supercleanmaster.adapter;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yzy.supercleanmaster.R;
import com.yzy.supercleanmaster.bean.AppProcessInfo;
import com.yzy.supercleanmaster.bean.AppProcsInfo;
import com.yzy.supercleanmaster.service.CoreService;
import com.yzy.supercleanmaster.utils.StorageUtil;

import java.util.ArrayList;
import java.util.List;

public class ClearMemoryAdapter2 extends BaseAdapter {

    public ArrayMap<String, AppProcsInfo> mAppInfoMap;
    LayoutInflater infater = null;
    private Context mContext;
    public static List<Integer> clearIds;
    private OnItemCheckedListener mOnItemCheckedListener;

    public interface OnItemCheckedListener {
        void onItemChecked(View v, boolean isChecked, long memory);
    }

    public ClearMemoryAdapter2(Context context, ArrayMap<String, AppProcsInfo> apps) {
        infater = LayoutInflater.from(context);
        mContext = context;
        clearIds = new ArrayList<Integer>();
        this.mAppInfoMap = apps;
    }

    @Override
    public int getCount() {
        return mAppInfoMap.size();
    }

    @Override
    public Object getItem(int position) {
        return mAppInfoMap.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public void setOnItemCheckedListener(OnItemCheckedListener listener) {
        mOnItemCheckedListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(CoreService.TAG, "ClearMemoryAdapter2.getView called ...");
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = infater.inflate(R.layout.listview_memory_clean,
                    null);
            holder = new ViewHolder();
            holder.appIcon = (ImageView) convertView
                    .findViewById(R.id.image);
            holder.appName = (TextView) convertView
                    .findViewById(R.id.name);
            holder.memory = (TextView) convertView
                    .findViewById(R.id.memory);

            holder.cb = (RadioButton) convertView
                    .findViewById(R.id.choice_radio);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final AppProcsInfo appInfo = (AppProcsInfo) getItem(position);
        Log.d(CoreService.TAG, "appInfo pkg =" + appInfo.pkgName);
        holder.appIcon.setImageDrawable(appInfo.icon);
        holder.appName.setText(appInfo.appName);
        holder.memory.setText(StorageUtil.convertStorage(appInfo.memory));
        if (appInfo.checked) {
            holder.cb.setChecked(true);
        } else {
            holder.cb.setChecked(false);
        }
        holder.cb.setOnClickListener(v->{
            if (appInfo.checked) {
                appInfo.checked = false;
            } else {
                appInfo.checked = true;
            }
            notifyDataSetChanged();
            if (null != mOnItemCheckedListener) {
                mOnItemCheckedListener.onItemChecked(v, appInfo.checked, appInfo.memory);
            }
        });

        return convertView;
    }

    class ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView memory;
        TextView tvProcessMemSize;
        RelativeLayout cb_rl;
        RadioButton cb;

        public RadioButton getCb() {
            return cb;
        }

        public void setCb(RadioButton cb) {
            this.cb = cb;
        }
    }
}
