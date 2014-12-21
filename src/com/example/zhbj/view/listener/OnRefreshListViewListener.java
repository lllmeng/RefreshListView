package com.example.zhbj.view.listener;

public interface OnRefreshListViewListener {

	/**
	 *  当ListView进入刷新的状态时调用此方法
	 */
	public void onRefresh();
	
	
	/** 
	 *  底部加载更多
	 */
	public void onLodingMore();
}
