package com.example.zhbj.view;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.refreshlistview.R;
import com.example.zhbj.view.listener.OnRefreshListViewListener;

public class RefreshListView extends ListView implements OnScrollListener {

	/**
	 * 头布局的容器
	 */
	private LinearLayout llHeadView;
	/**
	 * 下拉刷新显示的进度条
	 */
	private ProgressBar mProgressBar;
	/**
	 * 下拉刷新的箭头
	 */
	private View mIvArrow;
	/**
	 * 下拉刷新显示的文件
	 */
	private TextView mTvDesc;
	/**
	 * 下拉刷新最后刷新的时间
	 */
	private TextView mLastUpdateTime;
	/**
	 * 下拉刷新的箭头动画
	 */
	private RotateAnimation pullDwonAnima;
	private RotateAnimation upRefreshAnima;
	/**
	 * 下拉刷新的头布局
	 */
	private View mPullDownRefreshView;
	/**
	 * 下拉头布局的高度
	 */
	private int mPullDownRefreshViewHeight;

	/**
	 * 下拉刷新头布局的 按下的Y位置
	 */
	private int downY = -1;

	/**
	 * 当前的刷新状态
	 */
	private RefreshMode currentMode = RefreshMode.PULL_DWON_REFRESH;
	/**
	 * 用户的头布局
	 */
	private View mCustomHeaderView;

	private int mListViewYOnScreen = -1; // ListView在屏幕中y轴的值
	/**
	 *  ListView的刷新监听
	 */
	private OnRefreshListViewListener listener;
	/**
	 *  底部布局的高度
	 */
	private int footerViewHeight;
	
	private boolean isLoadingMore =false ;
	/**
	 *  底部加载更多
	 */
	private View footerView;

	public RefreshListView(Context context) {
		super(context);
		initHeadView();
		initFooterView();
	}

	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initHeadView();
		initFooterView();
	}

	public RefreshListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initHeadView();
		initFooterView();
	}

	/**
	 *  初始化底部加载更多的布局
	 */
	private void initFooterView() {
		footerView = inflate(getContext(), R.layout.list_loding_more_footerview, null);
		footerView.measure(0, 0);
		footerViewHeight = footerView.getMeasuredHeight();
		footerView.setPadding(0, -footerViewHeight, 0, 0);
		addFooterView(footerView);
		setOnScrollListener(this);
	}

	/**
	 * 初始化下拉刷新的头布局
	 */
	private void initHeadView() {
		llHeadView = (LinearLayout) inflate(getContext(),
				R.layout.list_refresh_headview, null);
		mProgressBar = (ProgressBar) llHeadView
				.findViewById(R.id.pb_list_pull_down_referesh);
		mIvArrow = llHeadView
				.findViewById(R.id.iv_arrow_list_pull_down_referesh);
		mTvDesc = (TextView) llHeadView
				.findViewById(R.id.tv_desc_list_pull_down_refresh);
		mLastUpdateTime = (TextView) llHeadView
				.findViewById(R.id.tv_lastupdatetime_list_pull_down_refresh);
		mPullDownRefreshView = llHeadView
				.findViewById(R.id.ll_pull_dwon_refresh);
		// 测量头布局的高度
		mPullDownRefreshView.measure(0, 0);
		mPullDownRefreshViewHeight = mPullDownRefreshView.getMeasuredHeight();
		// 隐藏下拉刷新头布局，设置其上内边局为负布局的高度
		mPullDownRefreshView.setPadding(0, -mPullDownRefreshViewHeight, 0, 0);
		addHeaderView(llHeadView);
		initAnimation();
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downY = (int) ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			if(downY==-1){
				downY=(int) ev.getY();
			}
			System.out.println("refreshListView   ACTION_MOVE");
			if (currentMode == RefreshMode.REFRESHING) {
				// 当前正在刷新中, 跳出switch
				break;
			}
			// 判断添加的轮播图是否完全显示了, 如果没有完全显示,
			// 不执行下面下拉头的代码, 跳转switch语句, 执行父元素的touch事件.
			if (mCustomHeaderView != null) {
				int[] location = new int[2]; // 0位是x轴的值, 1位是y轴的值
				if (mListViewYOnScreen == -1) {
					// 获取Listview在屏幕中y轴的值.
					this.getLocationOnScreen(location);
					mListViewYOnScreen = location[1];
				}
				// 获取mCustomHeaderView在屏幕y轴的值.
				mCustomHeaderView.getLocationOnScreen(location);
				int mCustomHeaderViewYOnScreen = location[1];
				if (mListViewYOnScreen > mCustomHeaderViewYOnScreen) {
					break;
				}
			}
			int moveY = (int) ev.getY();
			int diffY = moveY - downY;
			// 如果 手指是向下移动，并且第一个可见的条目是0的位置，就拉出头布局
			if (diffY > 0 && getFirstVisiblePosition() == 0) {
				int paddingTop = -mPullDownRefreshViewHeight + diffY;
				if (paddingTop < 0
						&& currentMode != RefreshMode.PULL_DWON_REFRESH) {
					currentMode = RefreshMode.PULL_DWON_REFRESH;
					refreshPullDownHeadViewState();
				}
				if (paddingTop > 0
						&& currentMode != RefreshMode.RELEASE_REFRESH) {
					currentMode = RefreshMode.RELEASE_REFRESH;
					refreshPullDownHeadViewState();
				}
				mPullDownRefreshView.setPadding(0, paddingTop, 0, 0);
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
			downY = 0;
			if (currentMode == RefreshMode.PULL_DWON_REFRESH) {
				System.out.println("下拉刷新中。。。。。。");
				mPullDownRefreshView.setPadding(0, -mPullDownRefreshViewHeight,
						0, 0);
			} else if (currentMode == RefreshMode.RELEASE_REFRESH) {
				System.out.println("释放刷新。。。。");
				mPullDownRefreshView.setPadding(0, 0, 0, 0);
				currentMode = RefreshMode.REFRESHING;
				refreshPullDownHeadViewState();
				if(listener!=null){
					listener.onRefresh();
				}
			}
			break;
		}
		return super.onTouchEvent(ev);
	}

	/**
	 * 刷新 下拉刷新头布局的状态
	 */
	private void refreshPullDownHeadViewState() {
		switch (currentMode) {
		case PULL_DWON_REFRESH:
			mTvDesc.setText("下拉刷新...");
			mIvArrow.startAnimation(pullDwonAnima);
			break;
		case RELEASE_REFRESH:
			mTvDesc.setText("释放刷新...");
			mIvArrow.startAnimation(upRefreshAnima);
			break;
		case REFRESHING:
			mTvDesc.setText("正在刷新...");
			mIvArrow.clearAnimation();
			mIvArrow.setVisibility(View.INVISIBLE);
			mProgressBar.setVisibility(View.VISIBLE);
			break;
		}
	}

	/**
	 * 初始化箭头的动画
	 */
	private void initAnimation() {
		upRefreshAnima = new RotateAnimation(0, -180,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		upRefreshAnima.setDuration(500);
		upRefreshAnima.setFillAfter(true);

		pullDwonAnima = new RotateAnimation(-180, -360,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		pullDwonAnima.setDuration(500);
		pullDwonAnima.setFillAfter(true);
	}

	/**
	 * 添加一个自定义的头布局
	 * 
	 * @param view
	 */
	public void addCustomHeadView(View view) {
		llHeadView.addView(view);
		mCustomHeaderView = view;
	}

	/**
	 * 下拉头部刷新的状态
	 */
	private enum RefreshMode {
		REFRESHING, PULL_DWON_REFRESH, RELEASE_REFRESH
	}
	
	/**
	 *  设置ListView的 刷新监听
	 * @param listener
	 */
	public void setOnRefreshListener(OnRefreshListViewListener listener){
		this.listener=listener;
	}
	
	/**
	 *  设置 ListView数据已经刷新完毕， 隐藏头布局
	 */
	public void OnRefreshCompleted(){
		if(isLoadingMore) {
			// 当前是加载更多的操作, 隐藏脚布局
			isLoadingMore = false;
			footerView.setPadding(0, -footerViewHeight, 0, 0);
		}else{
			mIvArrow.clearAnimation();
			mLastUpdateTime.setText("最后刷新时间："+getCurrentTime());
			mTvDesc.setText("下拉刷新...");
			mProgressBar.setVisibility(View.INVISIBLE);
			mIvArrow.setVisibility(View.VISIBLE);
			currentMode=RefreshMode.PULL_DWON_REFRESH;
			mPullDownRefreshView.setPadding(0, -mPullDownRefreshViewHeight, 0, 0);
		}
	}
	
	
	/**
	 *  获取当前的时间
	 * @return
	 */
	public String getCurrentTime(){
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date());
	}

	
	
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
	}
	/**
	 * ListView滚动监听
	 * @param view
	 * @param scrollState
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if(scrollState == SCROLL_STATE_IDLE 
				|| scrollState == SCROLL_STATE_FLING) {
			int lastVisiblePosition = getLastVisiblePosition();
			if((lastVisiblePosition == getCount() -1) && !isLoadingMore) {
				System.out.println("滑动到底部了");
				isLoadingMore  = true;
				footerView.setPadding(0, 0, 0, 0);
				// 把脚布局显示出来, 把ListView滑动到最低边
				this.setSelection(getCount());
				if(listener != null) {
					listener.onLodingMore();
				}
			}
		}
	}
}
