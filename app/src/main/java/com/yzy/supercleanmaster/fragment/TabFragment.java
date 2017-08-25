package com.yzy.supercleanmaster.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yzy.supercleanmaster.R;
import com.yzy.supercleanmaster.base.BaseFragment;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TabFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TabFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TabFragment extends BaseFragment {
    private TextView textView;
    private String title;

    /*一定要提供默认构造函数。不能用构造函数传递参数！不要写带参数的构造函数。参数通过下面介绍的方式传递。
      原因：Fragment会被重新销毁（Activity销毁的时候它里面的Fragment就被销毁了，可能因为内存不足，手机配置发生变化，横竖屏切换)。
      在重新创建的时候系统调用的是无参构造函数。
      标准做法是：
        在Fragment里添加获取Fragment的newInstance函数，以后获取Fragment就使用这个函数
        Fragment内部在初始化的时候需要获取外界传递的参数，这时候就用getArguments获取Bundle，再从Bundle里获取对应的参数。
        Bundle在Fragment销毁和重新创建的时候持续保存。

    public static MyFragment newInstance(Bundle args) {
        MyFragment f = new MyFragment();
        f.setArguments(args);
        return f;
    }*/

    public static TabFragment newInstance(int index){
        Bundle bundle = new Bundle();
        bundle.putInt("index", 'A' + index);
        TabFragment fragment = new TabFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected View initViews(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    //public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_fragment, null);
        textView = (TextView) view.findViewById(R.id.text);
        title = String.valueOf((char) getArguments().getInt("index"));
        textView.setText(String.valueOf((char) getArguments().getInt("index")));

        Log.i("CleanMaster", "TabFragment.initViews-" + title);

        return view;
    }

    @Override
    protected void initData() {
        Log.i("CleanMaster", "TabFragment.initData() called");
    }

    @Override
    public boolean getUserVisibleHint() {
        Log.i("CleanMaster", "TabFragment.getUserVisibleHint() called");
        return super.getUserVisibleHint();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i("CleanMaster", "TabFragment.onDestroyView-" + title);
    }
}
