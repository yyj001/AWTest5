package com.ish.awtest2.singleTouch;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ish.awtest2.R;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

public class SingleTouchTrainActivity extends WearableActivity implements DiscreteSeekBar.OnProgressChangeListener{

    private TextView mTextView;
    private DiscreteSeekBar seekBar;
    private int value = -1;
    private int range = -1;
    private SharedPreferences p = null;
    private Button trainBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_touch_train);
        setAmbientEnabled();
        initData();
    }

    private void initData(){
        seekBar = (DiscreteSeekBar)findViewById(R.id.st_seekbar);
        trainBtn = (Button)findViewById(R.id.st_train_btn);
        seekBar.setOnProgressChangeListener(this);
        //取出设置
        p = getApplicationContext().getSharedPreferences("Myprefs",
                Context.MODE_PRIVATE);
        range = p.getInt("range", range);
        seekBar.setMax(range);
        seekBar.setMin(0);
        value = p.getInt("value", value);
        //初始设置
        if(value==-1){
            value = range/2;
        }
        seekBar.setProgress(value);

        trainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                p.edit().putInt("value", value).apply();
                Intent intent = new Intent(SingleTouchTrainActivity.this, ConfirmationActivity.class);
                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                        ConfirmationActivity.SUCCESS_ANIMATION);
                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                        getString(R.string.msg_sent));
                startActivity(intent);
            }
        });
    }

    @Override
    public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
        this.value = value;
    }

    @Override
    public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

    }

}
