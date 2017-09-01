package com.yzy.supercleanmaster.base;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yzy.supercleanmaster.utils.T;


@SuppressLint("NewApi")
public abstract class BaseFragment extends Fragment {
	private boolean isVisible = false;
	private boolean isPrepared = false;
	private boolean isFirstLoad = false;

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (getUserVisibleHint()) {
			isVisible = true;
			Log.d("CleanMaster", "setUserVisibleHint() called,isVisible=" + isVisible);
			onVisible();
		} else {
			isVisible = false;
			Log.d("CleanMaster", "setUserVisibleHint() called,isVisible=" + isVisible);
			onInvisible();
		}
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		Log.d("CleanMaster", "onHiddenChanged() called");
		if (!hidden) {
			isVisible = true;
			onVisible();
		} else {
			isVisible = false;
			onInvisible();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//return super.onCreateView(inflater, container, savedInstanceState);
		isFirstLoad = true;
		Log.i("CleanMaster", "BaseFragment.onCreateView isFirstLoad=" + isFirstLoad);
		return initViews(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		isPrepared = true;
		Log.i("CleanMaster", "BaseFragment.onActivityCreated isPrepared=" + isPrepared);
		lazyLoad();
	}

	protected void lazyLoad() {
		Log.d("CleanMaster", "lazyLoad() called");
		Log.d("CleanMaster", "isPrepared=" + isPrepared + ",isVisible=" + isVisible + ",isFirstLoad=" + isFirstLoad);
		if (isPrepared && isVisible && isFirstLoad) {
			initData();
			isFirstLoad = false;
		}
	}

	/** 通过Class跳转界面 **/
	protected void startActivity(Class<?> cls) {
		startActivity(cls, null);
	}

	/** 含有Bundle通过Class跳转界面 **/
	protected void startActivity(Class<?> cls, Bundle bundle) {
		Intent intent = new Intent();
		intent.setClass(getActivity(), cls);
		if (bundle != null) {
			intent.putExtras(bundle);
		}
		startActivity(intent);
	}

	/** 通过Action跳转界面 **/
	protected void startActivity(String action) {
		startActivity(action, null);
	}

	/** 含有Bundle通过Action跳转界面 **/
	protected void startActivity(String action, Bundle bundle) {
		Intent intent = new Intent();
		intent.setAction(action);
		if (bundle != null) {
			intent.putExtras(bundle);
		}
		startActivity(intent);
	}

	@Override
	public void onDestroy() {//被ViewPager移出FragmentManager但实体类没有从内存销毁,变量需要重新初始化
		isFirstLoad = false;
		isPrepared = false;
		super.onDestroy();
	}

	/**
	 * 吐司
	 * 
	 * @param message
	 */
	protected void showShort(String message) {
		T.showShort(getActivity(), message);
	}

	protected void showLong(String message) {
		T.showLong(getActivity(), message);
	}

	protected abstract View initViews(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);
	protected abstract void initData();
	protected void onVisible() { lazyLoad(); }
	protected void onInvisible() {}
}
