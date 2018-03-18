package com.ish.awtest2.singleTouch;

import android.content.Intent;
import android.os.Bundle;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ish.awtest2.R;
import com.ish.awtest2.adapter.MyAdapter;
import com.ish.awtest2.bean.KnockData;
import com.ish.awtest2.bean.SettingItem;
import com.ish.awtest2.mview.CustomScrollingLayoutCallback;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;


public class STSettingActivity extends WearableActivity {
    private WearableRecyclerView mRecyclerView;
    private List<SettingItem> settingItemList = new ArrayList<SettingItem>();
    private String TAG = "STSetting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stsetting);
        initData();
    }

    private void initData() {
        SettingItem item1 = new SettingItem("Test", R.drawable.tap);
        settingItemList.add(item1);
        SettingItem item2 = new SettingItem("Add User", R.drawable.add);
        settingItemList.add(item2);
        SettingItem item3 = new SettingItem("User Management", R.drawable.delete);
        settingItemList.add(item3);
        SettingItem item4 = new SettingItem("Show Database", R.drawable.database);
        settingItemList.add(item4);
        SettingItem item5 = new SettingItem("Difficulty Setting", R.drawable.setting);
        settingItemList.add(item5);

        //recyclerView init
        mRecyclerView = (WearableRecyclerView) findViewById(R.id.st_recycler_view);
        mRecyclerView.setLayoutManager(
                new WearableLinearLayoutManager(this));
        mRecyclerView.setEdgeItemsCenteringEnabled(true);
        CustomScrollingLayoutCallback customScrollingLayoutCallback =
                new CustomScrollingLayoutCallback();
        WearableLinearLayoutManager manager = new WearableLinearLayoutManager(this, customScrollingLayoutCallback);
        mRecyclerView.setLayoutManager(manager);
        MyAdapter myAdapter = new MyAdapter(settingItemList);
        myAdapter.setOnItemClickListener(new MyAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position,String title) {
                startNewActivity(position);
            }
        });
        mRecyclerView.setAdapter(myAdapter);
    }

    private  void startNewActivity(int postion){
        switch (postion){
            case 0:
                startActivity(new Intent(STSettingActivity.this,StSelectUserActivity.class));
                break;
            case 1:
                startActivity(new Intent(STSettingActivity.this,MainActivity.class));
                break;
            case 2:
                startActivity(new Intent(STSettingActivity.this,StDeleteActivity.class));
                break;
            case 3:
                showDatabase();
                Toast.makeText(STSettingActivity.this, "sucessful", Toast.LENGTH_SHORT).show();
                break;
            case 4:
                startActivity(new Intent(STSettingActivity.this,SingleTouchTrainActivity.class));
                break;
                default:
        }
    }

    public void showDatabase() {
        List<KnockData> allDatas = DataSupport.findAll(KnockData.class);
        Log.d(TAG, "行数: " + allDatas.size() );
        for (KnockData row : allDatas) {
            Double[] rowData = row.getArray();
            String tempStr = row.getUserName();
            for (int i = 0; i < rowData.length; i++) {
                tempStr += "," + rowData[i];
            }
            Log.d(TAG, "showDatabase: " + tempStr);
            tempStr = "";
        }
    }
}
