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
	 * ͷ���ֵ�����
	 */
	private LinearLayout llHeadView;
	/**
	 * ����ˢ����ʾ�Ľ�����
	 */
	private ProgressBar mProgressBar;
	/**
	 * ����ˢ�µļ�ͷ
	 */
	private View mIvArrow;
	/**
	 * ����ˢ����ʾ���ļ�
	 */
	private TextView mTvDesc;
	/**
	 * ����ˢ�����ˢ�µ�ʱ��
	 */
	private TextView mLastUpdateTime;
	/**
	 * ����ˢ�µļ�ͷ����
	 */
	private RotateAnimation pullDwonAnima;
	private RotateAnimation upRefreshAnima;
	/**
	 * ����ˢ�µ�ͷ����
	 */
	private View mPullDownRefreshView;
	/**
	 * ����ͷ���ֵĸ߶�
	 */
	private int mPullDownRefreshViewHeight;

	/**
	 * ����ˢ��ͷ���ֵ� ���µ�Yλ��
	 */
	private int downY = -1;

	/**
	 * ��ǰ��ˢ��״̬
	 */
	private RefreshMode currentMode = RefreshMode.PULL_DWON_REFRESH;
	/**
	 * �û���ͷ����
	 */
	private View mCustomHeaderView;

	private int mListViewYOnScreen = -1; // ListView����Ļ��y���ֵ
	/**
	 *  ListView��ˢ�¼���
	 */
	private OnRefreshListViewListener listener;
	/**
	 *  �ײ����ֵĸ߶�
	 */
	private int footerViewHeight;
	
	private boolean isLoadingMore =false ;
	/**
	 *  �ײ����ظ���
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
	 *  ��ʼ���ײ����ظ���Ĳ���
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
	 * ��ʼ������ˢ�µ�ͷ����
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
		// ����ͷ���ֵĸ߶�
		mPullDownRefreshView.measure(0, 0);
		mPullDownRefreshViewHeight = mPullDownRefreshView.getMeasuredHeight();
		// ��������ˢ��ͷ���֣����������ڱ߾�Ϊ�����ֵĸ߶�
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
				// ��ǰ����ˢ����, ����switch
				break;
			}
			// �ж���ӵ��ֲ�ͼ�Ƿ���ȫ��ʾ��, ���û����ȫ��ʾ,
			// ��ִ����������ͷ�Ĵ���, ��תswitch���, ִ�и�Ԫ�ص�touch�¼�.
			if (mCustomHeaderView != null) {
				int[] location = new int[2]; // 0λ��x���ֵ, 1λ��y���ֵ
				if (mListViewYOnScreen == -1) {
					// ��ȡListview����Ļ��y���ֵ.
					this.getLocationOnScreen(location);
					mListViewYOnScreen = location[1];
				}
				// ��ȡmCustomHeaderView����Ļy���ֵ.
				mCustomHeaderView.getLocationOnScreen(location);
				int mCustomHeaderViewYOnScreen = location[1];
				if (mListViewYOnScreen > mCustomHeaderViewYOnScreen) {
					break;
				}
			}
			int moveY = (int) ev.getY();
			int diffY = moveY - downY;
			// ��� ��ָ�������ƶ������ҵ�һ���ɼ�����Ŀ��0��λ�ã�������ͷ����
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
				System.out.println("����ˢ���С�����������");
				mPullDownRefreshView.setPadding(0, -mPullDownRefreshViewHeight,
						0, 0);
			} else if (currentMode == RefreshMode.RELEASE_REFRESH) {
				System.out.println("�ͷ�ˢ�¡�������");
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
	 * ˢ�� ����ˢ��ͷ���ֵ�״̬
	 */
	private void refreshPullDownHeadViewState() {
		switch (currentMode) {
		case PULL_DWON_REFRESH:
			mTvDesc.setText("����ˢ��...");
			mIvArrow.startAnimation(pullDwonAnima);
			break;
		case RELEASE_REFRESH:
			mTvDesc.setText("�ͷ�ˢ��...");
			mIvArrow.startAnimation(upRefreshAnima);
			break;
		case REFRESHING:
			mTvDesc.setText("����ˢ��...");
			mIvArrow.clearAnimation();
			mIvArrow.setVisibility(View.INVISIBLE);
			mProgressBar.setVisibility(View.VISIBLE);
			break;
		}
	}

	/**
	 * ��ʼ����ͷ�Ķ���
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
	 * ���һ���Զ����ͷ����
	 * 
	 * @param view
	 */
	public void addCustomHeadView(View view) {
		llHeadView.addView(view);
		mCustomHeaderView = view;
	}

	/**
	 * ����ͷ��ˢ�µ�״̬
	 */
	private enum RefreshMode {
		REFRESHING, PULL_DWON_REFRESH, RELEASE_REFRESH
	}
	
	/**
	 *  ����ListView�� ˢ�¼���
	 * @param listener
	 */
	public void setOnRefreshListener(OnRefreshListViewListener listener){
		this.listener=listener;
	}
	
	/**
	 *  ���� ListView�����Ѿ�ˢ����ϣ� ����ͷ����
	 */
	public void OnRefreshCompleted(){
		if(isLoadingMore) {
			// ��ǰ�Ǽ��ظ���Ĳ���, ���ؽŲ���
			isLoadingMore = false;
			footerView.setPadding(0, -footerViewHeight, 0, 0);
		}else{
			mIvArrow.clearAnimation();
			mLastUpdateTime.setText("���ˢ��ʱ�䣺"+getCurrentTime());
			mTvDesc.setText("����ˢ��...");
			mProgressBar.setVisibility(View.INVISIBLE);
			mIvArrow.setVisibility(View.VISIBLE);
			currentMode=RefreshMode.PULL_DWON_REFRESH;
			mPullDownRefreshView.setPadding(0, -mPullDownRefreshViewHeight, 0, 0);
		}
	}
	
	
	/**
	 *  ��ȡ��ǰ��ʱ��
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
	 * ListView��������
	 * @param view
	 * @param scrollState
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if(scrollState == SCROLL_STATE_IDLE 
				|| scrollState == SCROLL_STATE_FLING) {
			int lastVisiblePosition = getLastVisiblePosition();
			if((lastVisiblePosition == getCount() -1) && !isLoadingMore) {
				System.out.println("�������ײ���");
				isLoadingMore  = true;
				footerView.setPadding(0, 0, 0, 0);
				// �ѽŲ�����ʾ����, ��ListView��������ͱ�
				this.setSelection(getCount());
				if(listener != null) {
					listener.onLodingMore();
				}
			}
		}
	}
}
