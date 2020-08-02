package com.example.myapplication.temp;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.SharedElementCallback;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.example.myapplication.R;
import com.example.myapplication.imkeyboard.AnyToolsKt;

import java.util.List;
import java.util.Map;

import static com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP;
import static com.example.myapplication.temp.Constant.IMAGE_ARRAY;


public class SecondActivity extends AppCompatActivity {

    private ViewPager2 mViewPager;
    private Adapter mAdapter;
    private View mCurrentView;
    private int mCurrentPosition;
    private int mStartPosition;
    private boolean mIsReturning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        postponeEnterTransition();
//        getWindow().setSharedElementEnterTransition(AnyToolsKt.getSsharedElementReturnTransition());
//        getWindow().setSharedElementExitTransition(AnyToolsKt.getSsharedElementReturnTransition());
//        getWindow().setSharedElementReenterTransition(AnyToolsKt.getSsharedElementReturnTransition());
//        getWindow().setSharedElementReturnTransition(AnyToolsKt.getSsharedElementReturnTransition());
//        setEnterSharedElementCallback(mCallback);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mStartPosition = getIntent().getIntExtra(Constant.EXTRA_START_POSITION, 0);
        if (savedInstanceState == null) {
            mCurrentPosition = mStartPosition;
        } else {
            mCurrentPosition = savedInstanceState.getInt(Constant.EXTRA_CURRENT_POSITION);
        }
        mAdapter = new Adapter();
        mViewPager = (ViewPager2) findViewById(R.id.viewpager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(mStartPosition, false);
//        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
//            @Override
//            public void onPageSelected(int position) {
//                mCurrentPosition = position;
//            }
//        });

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Constant.EXTRA_CURRENT_POSITION, mCurrentPosition);
    }

    SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mIsReturning) {
                ImageView imageView = (ImageView) mCurrentView.findViewById(R.id.item_imageview);
                Rect rect = new Rect();
                getWindow().getDecorView().getHitRect(rect);
                if (imageView.getLocalVisibleRect(rect)) {
                    if (imageView == null) {
                        names.clear();
                        sharedElements.clear();
                    } else if (mStartPosition != mCurrentPosition) {
                        names.clear();
                        names.add(imageView.getTransitionName());
                        sharedElements.clear();
                        sharedElements.put(imageView.getTransitionName(), imageView);
                    }
                }
            }
        }
    };

    private class VH extends RecyclerView.ViewHolder {

        public VH(@NonNull View itemView) {
            super(itemView);
        }
    }

    private class Adapter extends RecyclerView.Adapter<VH> {

        LayoutInflater mLayoutInflater;
        SubsamplingScaleImageView mImageView;

        public Adapter() {
            mLayoutInflater = LayoutInflater.from(SecondActivity.this);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(mLayoutInflater.inflate(R.layout.item_viewpager, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            View view = holder.itemView;
            mImageView = (SubsamplingScaleImageView) view.findViewById(R.id.item_imageview);
            mImageView.setTransitionName(Constant.TRANSITION_NAME + position);
//            mImageView.setMinimumScaleType(SCALE_TYPE_CENTER_CROP);
//            Drawable drawable = getResources().getDrawable(IMAGE_ARRAY[position % IMAGE_ARRAY.length]);
            mImageView.setImage(ImageSource.resource(IMAGE_ARRAY[position % IMAGE_ARRAY.length]));
            if (position == mStartPosition) {
                mImageView.setOnImageEventListener(new SubsamplingScaleImageView.DefaultOnImageEventListener(){
                    @Override
                    public void onImageLoaded() {
                        super.onImageLoaded();
                    }

                    @Override
                    public void onImageLoadError(Exception e) {
                        super.onImageLoadError(e);
                    }
                });
                mImageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                        startPostponedEnterTransition();
                        return true;
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return IMAGE_ARRAY.length * 5;
        }


//        @Override
//        public Object instantiateItem(ViewGroup container, int position) {
//            View view = mLayoutInflater.inflate(R.layout.item_viewpager, container, false);
//            mImageView = (ImageView) view.findViewById(R.id.item_imageview);
//            mImageView.setTransitionName(Constant.TRANSITION_NAME + position);
//            Drawable drawable = getResources().getDrawable(IMAGE_ARRAY[position% IMAGE_ARRAY.length] );
//            mImageView.setImageDrawable(drawable);
//            if (position == mStartPosition) {
//                mImageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//                    @Override
//                    public boolean onPreDraw() {
//                        mImageView.getViewTreeObserver().removeOnPreDrawListener(this);
//                        startPostponedEnterTransition();
//                        return true;
//                    }
//                });
//            }
//            container.addView(view);
//            return view;
//        }
//
//
//        @Override
//        public void destroyItem(ViewGroup container, int position, Object object) {
//            container.removeView((View) object);
//        }
//
//        @Override
//        public void setPrimaryItem(ViewGroup container, int position, Object object) {
//            mCurrentView = (View) object;
//        }
//
//        @Override
//        public int getCount() {
//            return IMAGE_ARRAY.length * 5;
//        }
//
//        @Override
//        public boolean isViewFromObject(View view, Object object) {
//            return view == object;
//        }


    }

    @Override
    public void finishAfterTransition() {

        mIsReturning = true;
        Intent intent = new Intent();
        intent.putExtra(Constant.EXTRA_START_POSITION, mStartPosition);
        intent.putExtra(Constant.EXTRA_CURRENT_POSITION, mCurrentPosition);
        setResult(RESULT_OK, intent);
        super.finishAfterTransition();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            supportFinishAfterTransition();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
