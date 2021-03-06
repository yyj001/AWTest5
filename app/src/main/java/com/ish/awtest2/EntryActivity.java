package com.ish.awtest2;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.ish.awtest2.pincode.PcSettingActivity;
import com.ish.awtest2.pincode.PinCodeMainActivity;
import com.ish.awtest2.singleTouch.MainActivity;
import com.ish.awtest2.singleTouch.STSettingActivity;

public class EntryActivity extends Activity {

    private TextView singelTouchTextView;
    private TextView pinCodeTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        singelTouchTextView = (TextView)findViewById(R.id.single_touch_tx);
        pinCodeTextView = (TextView)findViewById(R.id.pin_code_tx);
        singelTouchTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EntryActivity.this,STSettingActivity.class));
            }
        });
        pinCodeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EntryActivity.this,PcSettingActivity.class));
            }
        });
    }

}
