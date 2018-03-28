package com.ish.awtest2.singleTouch;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.Image;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ish.awtest2.R;
import com.ish.awtest2.bean.KnockData;
import com.ish.awtest2.bean.MyAudioData;
import com.ish.awtest2.func.Cut;
import com.ish.awtest2.func.FFT;
import com.ish.awtest2.func.Filter;
import com.ish.awtest2.func.GCC;
import com.ish.awtest2.func.IIRFilter;
import com.ish.awtest2.func.KNNAlgorithm;
import com.ish.awtest2.func.LimitQueue;
import com.ish.awtest2.func.Trainer;
import com.ish.awtest2.mview.TickView;

import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.List;

import static android.media.AudioRecord.READ_NON_BLOCKING;
import static android.view.View.GONE;

/**
 * @author ish
 */
public class TestActivity extends WearableActivity implements SensorEventListener {

    private TextView mTextViewCount;
    private Button btn;

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
    private int range = 10;
    /**
     * []data 队列转数组
     */
    private Double[] datax = null;
    private Double[] datay = null;
    private Double[] dataz = null;
    /**
     * deviation 振动改变阈值
     */
    private double deviation = 0.35;
    /**
     * knockCount 记录敲击次数
     */
    private int knockCount = 0;
    /**
     * firstKnock 记录敲击次数
     */
    private int ampLength = 32;
    private int finalLength = 144;
    //    private int audioLength = 1024;
//    private int finalAudioLength = 1024;
    private Double[] firstKnockx = new Double[ampLength];
    private Double[] firstKnocky = new Double[ampLength];
    private Double[] firstKnockz = new Double[ampLength];
    private Double[] finalData = new Double[finalLength];
    //    private Double[] firstAudioData = new Double[audioLength];
//    private Double[] finalAudioData = new Double[finalAudioLength];
    private KNNAlgorithm knnAlgorithm;
    /**
     * 训练距离
     */
    float threshold = 0;
    float threshold2 = 0;
    /**
     * 训练数据
     */
    Double[][] trainData;
    Double[][] audioTrainData;
    private static final String TAG = "sensorTest";
    private String s = "";
    //
    private TickView tickView;
    private ImageView fingerImage;

    private double newDis1, newDis2;
    //动画
    Animation disappearAnimation;

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            recLen++;
            if (recLen == 1) {
                mTextViewCount.setText("READY");
            } else if (recLen > 2) {
                mTextViewCount.setText("TAP YOUR HAND");
            }
            handler.postDelayed(this, 2000);
        }
    };

    /**
     * 音频数据
     */
    private boolean ifsaveAudio = false;
    private int frequency = 11025;
    private int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding) * 20;
    private short[] rawAudioData = new short[bufferSize];
    private AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
            frequency, channelConfiguration, audioEncoding, bufferSize);
    private int bufferResultLength;
    //如果不每隔一秒就把音频保存一下就会有问题
    private Runnable audioRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void run() {
            bufferResultLength = audioRecord.read(rawAudioData, 0, bufferSize, READ_NON_BLOCKING);
            Log.d(TAG, "run: +bufferlength " + bufferResultLength);
            handler.postDelayed(this, 1000);
        }
    };
    private boolean ifsaveAmp = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        iniView();
        SQLiteDatabase db = Connector.getDatabase();
    }

    /**
     * 初始化数据
     */
    public void iniView() {
        tickView = (TickView) findViewById(R.id.tick_view_test);
        fingerImage = (ImageView) findViewById(R.id.finger_image);
        //慢慢消失动画
        disappearAnimation = new AlphaAnimation(1, 0);
        disappearAnimation.setDuration(500);

        disappearAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //tickView.setVisibility(View.GONE);
                tickView.setChecked(false);
                tickView.setAlpha(0);
                fingerImage.setVisibility(View.VISIBLE);
            }
        });

        btn = (Button) findViewById(R.id.test_btn);
        mTextViewCount = (TextView) findViewById(R.id.test_text_count);


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
                    btn.setText("START");
                } else {
                    fingerImage.setVisibility(View.VISIBLE);
                    handler.postDelayed(runnable, 1000);
                    recLen = 0;
                    flag = true;
                    btn.setText("STOP");
                    btn.setAlpha(0);
                    tickView.setType(TickView.TYPE_ERROR);
                    tickView.setChecked(true);
                    btn.setClickable(false);
                }
            }
        });

        //取阈值
        SharedPreferences p = getApplicationContext().getSharedPreferences("Myprefs",
                Context.MODE_PRIVATE);
        threshold = 0;
        //取出训练数据
        String userName = getIntent().getStringExtra("userName");
        List<KnockData> allDatas = DataSupport.where("userName = ?", userName).find(KnockData.class);
        trainData = new Double[allDatas.size()][finalLength];
        int r = 0;
        for (KnockData row : allDatas) {
            trainData[r] = row.getArray();
            r++;
        }
        //初始化第一个来对齐
        System.arraycopy(trainData[0], 0, firstKnockx, 0, ampLength);
        System.arraycopy(trainData[0], ampLength, firstKnocky, 0, ampLength);
        System.arraycopy(trainData[0], ampLength * 2, firstKnockz, 0, ampLength);

        // get it
        int value = -1;
        value = p.getInt("value", value);
        range = p.getInt("range", range);
        //没有初始化
        if (value == -1) {
            value = range / 2;
        }
        //p.edit().putFloat("threshold", (float) threshold).apply();

        knnAlgorithm = new KNNAlgorithm(trainData);
        knnAlgorithm.setThreshold(value, range);


        double threshold = knnAlgorithm.getThreshold();
        Log.d(TAG, "onClick: threshold" + threshold);

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
            //mTextView.setText(z + "");
            //判断是否存了200个点
            if (!ifStart) {
                if (count == limit) {
                    ifStart = true;
                }
            }
            //等待敲击
            else {
                //遇到敲击
                if (zChange > deviation && !ifStart2 && count > 210) {
                    //SaveAudioRunnable myThread = new SaveAudioRunnable();
                    //new Thread(myThread).start();
                    ifStart2 = true;
                    count = 0;
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
                    datax = xQueue.toArray(new Double[limit]);
                    datay = yQueue.toArray(new Double[limit]);
                    dataz = zQueue.toArray(new Double[limit]);

                    datax = IIRFilter.highpass(datax, IIRFilter.TYPE_AMPITUDE);
                    datax = IIRFilter.lowpass(datax, IIRFilter.TYPE_AMPITUDE);

                    datay = IIRFilter.highpass(datay, IIRFilter.TYPE_AMPITUDE);
                    datay = IIRFilter.lowpass(datay, IIRFilter.TYPE_AMPITUDE);

                    dataz = IIRFilter.highpass(dataz, IIRFilter.TYPE_AMPITUDE);
                    dataz = IIRFilter.lowpass(dataz, IIRFilter.TYPE_AMPITUDE);

                    Double[] cutDatax = Cut.cutMoutain2(datax, 84, 35, 60, 1);
                    Double[] cutDatay = Cut.cutMoutain2(datay, 130, 35, 60, 2);
                    Double[] cutDataz = Cut.cutMoutain2(dataz, 84, 35, 60, 3);

                    boolean ifmove = false;
                    //判断是否为手臂晃动
                    if (cutDatax[0] == 0.0 || cutDatay[0] == 0.0 || cutDataz[0] == 0.0) {
                        ifmove = true;
                    }

                    if (!ifmove) {
                        Double[] gccDatax = GCC.gcc(firstKnockx, cutDatax);
                        Double[] gccDatay = GCC.gcc(firstKnocky, cutDatay);
                        Double[] gccDataz = GCC.gcc(firstKnockz, cutDataz);

                        //加上fft的,得到最终数据
                        Double[] fftDatax = FFT.getHalfFFTData(gccDatax);
                        Double[] fftDatay = FFT.getHalfFFTData(gccDatay);
                        Double[] fftDataz = FFT.getHalfFFTData(gccDataz);
                        System.arraycopy(gccDatax, 0, finalData, 0, ampLength);
                        System.arraycopy(gccDatay, 0, finalData, ampLength, ampLength);
                        System.arraycopy(gccDataz, 0, finalData, ampLength * 2, ampLength);
                        System.arraycopy(fftDatax, 0, finalData, ampLength * 3, ampLength / 2);
                        System.arraycopy(fftDatay, 0, finalData, ampLength * 3 + ampLength / 2, ampLength / 2);
                        System.arraycopy(fftDataz, 0, finalData, ampLength * 4, ampLength / 2);
                        for (int i = 0; i < finalData.length; i++) {
                            s = s + "," + finalData[i];
                        }
                        Log.d(TAG, "onSensorChanged: finalData" + s);
                        s = "";
                        //将新的敲击数据加入对比
                        //double newDis = Trainer.getNewDis(trainData, finalData);
                        boolean isme = knnAlgorithm.isMe(finalData);
                        //隐藏手指，显示动画
                        fingerImage.setVisibility(GONE);
                        tickView.setAlpha(1);
                        //失败
                        //if (threshold >= newDis) {
                        if (isme) {
                            tickView.setType(TickView.TYPE_SUCCESS);
                            tickView.setChecked(true);
                            mTextViewCount.setText("SUCESSED");
                            recLen = 2;
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    tickView.startAnimation(disappearAnimation);
                                }
                            }, 1500);
                        } else {
                            tickView.setType(TickView.TYPE_ERROR);
                            tickView.setChecked(true);
                            mTextViewCount.setText("TRY AGAIN");
                            recLen = 2;
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    tickView.startAnimation(disappearAnimation);
                                }
                            }, 1500);
                        }
                    }
                    flag = true;
                    ifsaveAmp = true;
                }
            }
        }
    }

//    private void getNewDis2(){
//        handler.removeCallbacks(audioRunnable);
//        bufferResultLength = audioRecord.read(rawAudioData, 0, bufferSize);
//        Log.d(TAG, "onClick: " + bufferResultLength);
//        Double[] filterAudioData = new Double[bufferResultLength];
//        for (int i = 0; i < bufferResultLength; i++) {
//            filterAudioData[i] = Double.valueOf(rawAudioData[i]);
//        }
//        filterAudioData = Filter.highpass(filterAudioData);
//        filterAudioData = Filter.lowpass(filterAudioData);
//        Double[] cutAudioData = Cut.cutMoutain(filterAudioData, 1500, 2000, 700, 4, 140);
//        finalAudioData = GCC.gcc(firstAudioData, cutAudioData);
//        int temp = finalAudioLength / 8;
//        for (int i = 0; i < 8; i++) {
//            for (int j = 0; j < temp; j++) {
//                s = s + "," + finalAudioData[j + temp * i];
//            }
//            Log.d(TAG, "radio: " + s);
//            s = "";
//        }
//        //waitting
//        while(!ifsaveAmp){
//            Log.d(TAG, "waitting...........");
//        }
//        ifsaveAmp = false;
//         newDis1 = Trainer.getNewDis(trainData, finalData);
//         newDis2 = Trainer.getNewDis(audioTrainData, finalData);
//        Log.d(TAG, "dis1: " + newDis1);
//        Log.d(TAG, "dis2: " + newDis2);
//        //隐藏手指，显示动画
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                fingerImage.setVisibility(GONE);
//                tickView.setAlpha(1);
//                //失败
//                if (threshold >= newDis1 && threshold2 >= newDis2) {
//                    tickView.setType(TickView.TYPE_SUCCESS);
//                    tickView.setChecked(true);
//                    mTextViewCount.setText("SUCESSED");
//                    recLen = 2;
//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            tickView.startAnimation(disappearAnimation);
//                        }
//                    }, 1500);
//                } else {
//                    tickView.setType(TickView.TYPE_ERROR);
//                    tickView.setChecked(true);
//                    mTextViewCount.setText("TRY AGAIN");
//                    recLen = 2;
//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            tickView.startAnimation(disappearAnimation);
//                        }
//                    }, 1500);
//                }
//
//            }
//        });
//
//        handler.post(audioRunnable);
//    }


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

    }

    @Override
    public void onPause() {
        sm.unregisterListener(this);
        super.onPause();
    }
}
