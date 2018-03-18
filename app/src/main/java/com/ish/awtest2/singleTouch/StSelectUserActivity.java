package com.ish.awtest2.singleTouch;

import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;
import android.widget.TextView;

import com.ish.awtest2.R;
import com.ish.awtest2.adapter.MyAdapter;
import com.ish.awtest2.bean.KnockData;
import com.ish.awtest2.bean.SettingItem;
import com.ish.awtest2.mview.CustomScrollingLayoutCallback;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StSelectUserActivity extends WearableActivity {
    private int[] iconArray  = {
            R.drawable.p0,
            R.drawable.p1,
            R.drawable.p2,
            R.drawable.p3,
            R.drawable.p4,
            R.drawable.p5,
            R.drawable.p6,
            R.drawable.p7,
            R.drawable.p8,
            R.drawable.p9,
            R.drawable.p10
    };
    private WearableRecyclerView mRecyclerView;
    private List<SettingItem> settingItemList = new ArrayList<SettingItem>();
    private String TAG = "STSetting";
    private MyAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_st_select_user);
        // Enables Always-on
        setAmbientEnabled();
        initData();
    }

    private void initData() {
        //get nameArray;
        Random rnd = new Random();
        Cursor cursor = DataSupport.findBySQL("select distinct userName from KnockData");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String userName = cursor.getString(0);
                //随机头像
                int index = rnd.nextInt(11);
                SettingItem item = new SettingItem(userName, iconArray[index]);
                settingItemList.add(item);
            }while(cursor.moveToNext());
        }

        //recyclerView init
        mRecyclerView = (WearableRecyclerView) findViewById(R.id.st_select_recycler_view);
        mRecyclerView.setLayoutManager(
                new WearableLinearLayoutManager(this));
        //mRecyclerView.setEdgeItemsCenteringEnabled(true);
        CustomScrollingLayoutCallback customScrollingLayoutCallback =
                new CustomScrollingLayoutCallback();
        WearableLinearLayoutManager manager = new WearableLinearLayoutManager(this, customScrollingLayoutCallback);
        mRecyclerView.setLayoutManager(manager);
        myAdapter = new MyAdapter(settingItemList);
        //delete action
        myAdapter.setOnItemClickListener(new MyAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position,String name) {
                Intent intent = new Intent(StSelectUserActivity.this,TestActivity.class);
                intent.putExtra("userName",name);
                startActivity(intent);
            }
        });
        mRecyclerView.setAdapter(myAdapter);
    }
}
