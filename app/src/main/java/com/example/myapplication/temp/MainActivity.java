package com.example.myapplication.temp;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.SharedElementCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.List;
import java.util.Map;

import static com.example.myapplication.temp.Constant.IMAGE_ARRAY;


public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private Bundle mTmpReenterState;
    private MyAdapter mMyAdapter;
    private LinearLayoutManager mLinearLayoutManager;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);
        setExitSharedElementCallback(mCallback);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMyAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mMyAdapter);
    }

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mTmpReenterState != null) {
                int startingPosition = mTmpReenterState.getInt(Constant.EXTRA_START_POSITION);
                int currentPosition = mTmpReenterState.getInt(Constant.EXTRA_CURRENT_POSITION);
                if (startingPosition != currentPosition) {
                    View newSharedElement = getNewSharedElement(currentPosition);
                    if (newSharedElement != null) {
                        names.clear();
                        names.add(Constant.TRANSITION_NAME + currentPosition);
                        sharedElements.clear();
                        sharedElements.put(Constant.TRANSITION_NAME + currentPosition, newSharedElement);
                    }
                }
                mTmpReenterState = null;
            } else {
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                }
                if (statusBar != null) {
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
            }
        }
    };

    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mTmpReenterState = data.getExtras();
        int startPosition = mTmpReenterState.getInt(Constant.EXTRA_START_POSITION);
        int currentPosition = mTmpReenterState.getInt(Constant.EXTRA_CURRENT_POSITION);
        rennter(startPosition, currentPosition);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    public ImageView getNewSharedElement(int position) {
        return (ImageView) mRecyclerView.findViewWithTag("tag" + position);
    }

    public void rennter(int startPosition, int currentPosition) {
        if (startPosition != currentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
        }
    }


    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        private LayoutInflater mLayoutInflater;


        public MyAdapter() {
            mLayoutInflater = LayoutInflater.from(MainActivity.this);
        }

        @Override
        public int getItemCount() {
            return IMAGE_ARRAY.length * 5;
        }


        @Override
        public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.item_recyclerview, parent, false);
            return new MyAdapter.MyViewHolder(view);
        }


        @Override
        public void onBindViewHolder(MyAdapter.MyViewHolder holder, int position) {
            Drawable drawable = getResources().getDrawable(IMAGE_ARRAY[position % IMAGE_ARRAY.length]);
            holder.imageView.setImageDrawable(drawable);
            holder.imageView.setTransitionName(Constant.TRANSITION_NAME + position);
            holder.imageView.setTag("tag" + position);

        }


        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            public ImageView imageView;

            public MyViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                imageView = (ImageView) itemView.findViewById(R.id.item_imageview);
            }

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                intent.putExtra(Constant.EXTRA_START_POSITION, getAdapterPosition());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(MainActivity.this, imageView, imageView.getTransitionName());
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        }
    }

}
