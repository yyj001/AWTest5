package com.ish.awtest2.singleTouch;

import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ish.awtest2.R;
import com.ish.awtest2.bean.KnockData;
import com.ish.awtest2.bean.MyAudioData;
import com.ish.awtest2.func.Cut;
import com.ish.awtest2.func.FFT;
import com.ish.awtest2.func.GCC;
import com.ish.awtest2.func.IIRFilter;
import com.ish.awtest2.func.KNNAlgorithm;
import com.ish.awtest2.func.LimitQueue;

import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.media.AudioRecord.READ_NON_BLOCKING;


public class MainActivity extends WearableActivity implements SensorEventListener {

    //view
    private TextView mTextView;
    private TextView mTextViewCount;
    private EditText editText;
    private Button btn;
    private SharedPreferences p;
    private int range = 10;
    private SensorManager sm;
    private double preValue = 0;
    /**
     * count记录初始队列点的数目
     * count2记录移位点的数目
     */
    private long count = 0;
    private long count2 = 0;

    /**
     * 倒计时，从-1开始，接受两秒空白
     */
    private int recLen = -1;

    /**
     * 设置队列长度
     */
    private int limit = 200;

    /**
     * 设置队列缓存
     */
    LimitQueue<Double> xQueue = new LimitQueue<Double>(limit);
    LimitQueue<Double> yQueue = new LimitQueue<Double>(limit);
    LimitQueue<Double> zQueue = new LimitQueue<Double>(limit);

    /**
     * flag 按钮判断开始
     * ifStart 录完200个点，可以开始敲击
     * ifStart2 敲击开始
     */
    private boolean flag = false;
    private boolean ifStart = false;
    private boolean ifStart2 = false;
    /**
     * []data 队列转数组
     */
    private Double[] datax = null;
    private Double[] datay = null;
    private Double[] dataz = null;
    /**
     * deviation 振动改变阈值
     */
    private double deviation = 0.5;
    /**
     * knockCount 记录敲击次数
     */
    private int knockCount = 0;
    /**
     * firstKnock 记录敲击次数
     */
    private int ampLength = 32;
    private int finalLength = 144;
    private Double[] firstKnockx = new Double[ampLength];
    private Double[] firstKnocky = new Double[ampLength];
    private Double[] firstKnockz = new Double[ampLength];
    private Double[] finalData = new Double[finalLength];

    private static final String TAG = "sensor";
    private String s = "";
    private ExecutorService executorService;


    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (recLen < 30) {
                recLen++;
                mTextViewCount.setText("" + recLen);
                handler.postDelayed(this, 2000);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android
                .Manifest.permission.RECORD_AUDIO}, 1);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        iniView();
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && flag) {
            double x = sensorEvent.values[0];
            double y = sensorEvent.values[1];
            double z = sensorEvent.values[2];
            double zChange = z - preValue;
            preValue = z;
            xQueue.offer(x);
            yQueue.offer(y);
            zQueue.offer(z);
            count++;
            mTextView.setText(z + "");
            //判断是否存了200个点
            if (!ifStart) {
                if (count == limit) {
                    ifStart = true;
                }
            }
            //等待敲击
            else {
                //遇到敲击
                if (zChange > deviation && !ifStart2) {
                    ifStart2 = true;
                    count = 0;
                    knockCount++;
                }
            }
            //开始左移100个点
            if (ifStart2) {
                //左移了100个点
                if (count == limit / 2) {
                    //停止接收直到处理完
                    flag = false;
                    ifStart2 = false;
                    //队列转数组
                    Toast.makeText(this, "" + knockCount, Toast.LENGTH_SHORT).show();
                    datax = xQueue.toArray(new Double[limit]);
                    datay = yQueue.toArray(new Double[limit]);
                    dataz = zQueue.toArray(new Double[limit]);

                    //加入线程池
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (this) {
                                datax = IIRFilter.highpass(datax, IIRFilter.TYPE_AMPITUDE);
                                datax = IIRFilter.lowpass(datax, IIRFilter.TYPE_AMPITUDE);

                                datay = IIRFilter.highpass(datay, IIRFilter.TYPE_AMPITUDE);
                                datay = IIRFilter.lowpass(datay, IIRFilter.TYPE_AMPITUDE);

                                dataz = IIRFilter.highpass(dataz, IIRFilter.TYPE_AMPITUDE);
                                dataz = IIRFilter.lowpass(dataz, IIRFilter.TYPE_AMPITUDE);
//                            for(int i=0;i<limit;i++){
//                                s+=","+data[i];
//                            }
//                            Log.d(TAG, "after filter,"+s);
//                            s = "";
                                Double[] cutDatax = Cut.cutMoutain2(datax, 64, 35, 40);
                                Double[] cutDatay = Cut.cutMoutain2(datay, 150, 40, 120);
                                Double[] cutDataz = Cut.cutMoutain2(dataz, 64, 35, 40);
                                //如果是第一个敲击，记录下来，后面的敲击gcc以它对齐
                                if (knockCount == 1) {
                                    System.arraycopy(cutDatax, 10, firstKnockx, 0, ampLength);
                                    System.arraycopy(cutDatay, 55, firstKnocky, 0, ampLength);
                                    System.arraycopy(cutDataz, 18, firstKnockz, 0, ampLength);
                                    Double[] fftDatax = FFT.getHalfFFTData(firstKnockx);
                                    Double[] fftDatay = FFT.getHalfFFTData(firstKnocky);
                                    Double[] fftDataz = FFT.getHalfFFTData(firstKnockz);
                                    System.arraycopy(firstKnockx, 0, finalData, 0, ampLength);
                                    System.arraycopy(firstKnocky, 0, finalData, ampLength, ampLength);
                                    System.arraycopy(firstKnockz, 0, finalData, ampLength * 2
                                            , ampLength);
                                    System.arraycopy(fftDatax, 0, finalData, ampLength * 3
                                            , ampLength / 2);
                                    System.arraycopy(fftDatay, 0, finalData, ampLength * 3 + ampLength / 2
                                            , ampLength / 2);
                                    System.arraycopy(fftDataz, 0, finalData, ampLength * 4
                                            , ampLength / 2);
                                } else {
                                    Double[] gccDatax = GCC.gcc(firstKnockx, cutDatax);
                                    Double[] gccDatay = GCC.gcc(firstKnocky, cutDatay);
                                    Double[] gccDataz = GCC.gcc(firstKnockz, cutDataz);
//                                for(int i=0;i<32;i++){
//                                    s+=","+gccData[i];
//                                }
//                                Log.d(TAG, "gcc,"+s);
//                                s = "";
                                    Double[] fftDatax = FFT.getHalfFFTData(gccDatax);
                                    Double[] fftDatay = FFT.getHalfFFTData(gccDatay);
                                    Double[] fftDataz = FFT.getHalfFFTData(gccDataz);

                                    System.arraycopy(gccDatax, 0, finalData, 0, ampLength);
                                    System.arraycopy(gccDatay, 0, finalData, ampLength, ampLength);
                                    System.arraycopy(gccDataz, 0, finalData, ampLength * 2
                                            , ampLength);
                                    System.arraycopy(fftDatax, 0, finalData, ampLength * 3
                                            , ampLength / 2);
                                    System.arraycopy(fftDatay, 0, finalData, ampLength * 3 + ampLength / 2
                                            , ampLength / 2);
                                    System.arraycopy(fftDataz, 0, finalData, ampLength * 4
                                            , ampLength / 2);
                                }
                                KnockData knockData = new KnockData();
                                knockData.initData(editText.getText().toString(), finalData);
                                knockData.saveThrows();
                            }
                        }
                    });
                    flag = true;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        //创建一个SensorManager来获取系统的传感器服务
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                , 10000);

        //初始化数据


    }

    public void iniView() {
        btn = (Button) findViewById(R.id.btn);
        mTextView = (TextView) findViewById(R.id.text);
        mTextViewCount = (TextView) findViewById(R.id.text_count);
        editText = (EditText) findViewById(R.id.st_name_edtxt);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //停止
                if (flag) {
                    ifStart = false;
                    flag = false;
                    count = 0;
                    s = "";
                    handler.removeCallbacks(runnable);
                    mTextViewCount.setText("0");
                    btn.setText("start");
                } else {
                    //判断是否已经输入名字
                    if (editText.getText().toString().trim().equals("")) {
                        Toast.makeText(MainActivity.this, "input user's name", Toast.LENGTH_SHORT).show();
                    } else {
                        handler.postDelayed(runnable, 200);
                        recLen = -1;
                        flag = true;
                        btn.setText("stop");
                        //knockCount = 0;
                    }
                }
            }
        });
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        int KEEP_ALIVE_TIME = 1;
        TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        executorService = new ThreadPoolExecutor(NUMBER_OF_CORES,
                NUMBER_OF_CORES * 2,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                taskQueue);

        //如果没有初始化过难度就初始化一下
        p = getApplicationContext().getSharedPreferences("Myprefs",
                Context.MODE_PRIVATE);
        int value = -1;
        value = p.getInt("value", value);
        if(value==-1){
            p.edit().putInt("value", 5).apply();
            p.edit().putInt("range", 10).apply();
        }
    }

    @Override
    public void onPause() {
        sm.unregisterListener(this);
        handler.removeCallbacks(runnable);
        super.onPause();
    }
}