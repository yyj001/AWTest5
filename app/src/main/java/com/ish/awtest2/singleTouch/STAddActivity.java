package com.ish.awtest2.singleTouch;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ish.awtest2.R;

public class STAddActivity extends WearableActivity{

    private  LinearLayout addBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stadd);
        setAmbientEnabled();
        initData();
    }

    public void initData(){
        addBtn = (LinearLayout)findViewById(R.id.st_add_data);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(STAddActivity.this,MainActivity.class));
            }
        });
    }

}
