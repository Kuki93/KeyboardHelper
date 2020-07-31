package com.example.myapplication;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;


import java.lang.reflect.Field;

public class ScrollSpeedManger extends LinearLayoutManager {

    private BannerHelper helper;

    private ScrollSpeedManger(Context context, BannerHelper helper, LinearLayoutManager linearLayoutManager) {
        super(context, linearLayoutManager.getOrientation(), false);
        this.helper = helper;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            protected int calculateTimeForDeceleration(int dx) {
                return helper.getScrollTime();
            }
        };
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    public static void reflectLayoutManager(BannerHelper helper, ViewPager2 viewPager2) {
        if (helper.getScrollTime() < 100) return;
        try {
            RecyclerView recyclerView = (RecyclerView) viewPager2.getChildAt(0);
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (linearLayoutManager == null) {
                return;
            }
            recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
            ScrollSpeedManger speedManger = new ScrollSpeedManger(recyclerView.getContext() ,helper, linearLayoutManager);
            recyclerView.setLayoutManager(speedManger);

            Field mRecyclerView = RecyclerView.LayoutManager.class.getDeclaredField("mRecyclerView");
            mRecyclerView.setAccessible(true);
            mRecyclerView.set(linearLayoutManager, recyclerView);

            Field LayoutMangerField = ViewPager2.class.getDeclaredField("mLayoutManager");
            LayoutMangerField.setAccessible(true);
            LayoutMangerField.set(viewPager2, speedManger);

            Field pageTransformerAdapterField = ViewPager2.class.getDeclaredField("mPageTransformerAdapter");
            pageTransformerAdapterField.setAccessible(true);
            Object mPageTransformerAdapter = pageTransformerAdapterField.get(viewPager2);
            if (mPageTransformerAdapter != null) {
                Class<?> aClass = mPageTransformerAdapter.getClass();
                Field layoutManager = aClass.getDeclaredField("mLayoutManager");
                layoutManager.setAccessible(true);
                layoutManager.set(mPageTransformerAdapter, speedManger);
            }
            Field scrollEventAdapterField = ViewPager2.class.getDeclaredField("mScrollEventAdapter");
            scrollEventAdapterField.setAccessible(true);
            Object mScrollEventAdapter = scrollEventAdapterField.get(viewPager2);
            if (mScrollEventAdapter != null) {
                Class<?> aClass = mScrollEventAdapter.getClass();
                Field layoutManager = aClass.getDeclaredField("mLayoutManager");
                layoutManager.setAccessible(true);
                layoutManager.set(mScrollEventAdapter, speedManger);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}