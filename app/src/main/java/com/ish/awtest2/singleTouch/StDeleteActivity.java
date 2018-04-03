package com.ish.awtest2.singleTouch;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wear.widget.CircularProgressLayout;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ish.awtest2.R;
import com.ish.awtest2.adapter.MyAdapter;
import com.ish.awtest2.bean.KnockData;
import com.ish.awtest2.bean.SettingItem;
import com.ish.awtest2.bean.StLabel;
import com.ish.awtest2.bean.StThresholds;
import com.ish.awtest2.bean.StWeight;
import com.ish.awtest2.mview.CustomScrollingLayoutCallback;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StDeleteActivity extends WearableActivity {
    private WearableRecyclerView mRecyclerView;
    private List<SettingItem> settingItemList = new ArrayList<SettingItem>();
    private String TAG = "STSetting";
    private String myName;
    private int pos = 0;
    private MyAdapter myAdapter;
    private Vibrator mVibrator;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_st_delete);
        // Enables Always-on
        setAmbientEnabled();
        initData();
    }

    private void initData() {
        mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
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
        mRecyclerView = (WearableRecyclerView) findViewById(R.id.st_delete_recycler_view);
        mRecyclerView.setLayoutManager(
                new WearableLinearLayoutManager(this));
        mRecyclerView.setEdgeItemsCenteringEnabled(true);
        CustomScrollingLayoutCallback customScrollingLayoutCallback =
                new CustomScrollingLayoutCallback();
        WearableLinearLayoutManager manager = new WearableLinearLayoutManager(this, customScrollingLayoutCallback);
        mRecyclerView.setLayoutManager(manager);
        myAdapter = new MyAdapter(settingItemList);
        //delete action
        myAdapter.setOnItemLongClickListener(new MyAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(int position,String name) {
                myName = name;
                pos = position;
                mVibrator.vibrate(new long[]{10, 10}, -1);

                AcceptDenyDialog dialog = new AcceptDenyDialog(StDeleteActivity.this);
                dialog.setIcon(R.drawable.delete);
                dialog.setTitle("Do you want to delete " + name +"'s data?");
                dialog.setCancelable(true);
                dialog.setPositiveButton(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DataSupport.deleteAll(KnockData.class, "userName = ?", myName);
                        DataSupport.deleteAll(StThresholds.class, "userName = ?", myName);
                        DataSupport.deleteAll(StLabel.class, "userName = ?", myName);
                        DataSupport.deleteAll(StWeight.class, "userName = ?", myName);
                        //移出动画
                        settingItemList.remove(pos);
                        myAdapter.notifyItemRemoved(pos);
                        myAdapter.notifyItemRangeChanged(0,settingItemList.size()-pos);

                        Intent intent = new Intent(StDeleteActivity.this, ConfirmationActivity.class);
                        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                ConfirmationActivity.SUCCESS_ANIMATION);
                        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                                getString(R.string.msg_sent));
                        startActivity(intent);                    }
                });
                dialog.setNegativeButton(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialog.show();

            }
        });
        mRecyclerView.setAdapter(myAdapter);
    }
}
