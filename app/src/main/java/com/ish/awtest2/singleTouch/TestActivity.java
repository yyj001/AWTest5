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
import com.ish.awtest2.bean.StLabel;
import com.ish.awtest2.bean.StThresholds;
import com.ish.awtest2.bean.StWeight;
import com.ish.awtest2.func.Cut;
import com.ish.awtest2.func.FFT;
import com.ish.awtest2.func.Filter;
import com.ish.awtest2.func.GCC;
import com.ish.awtest2.func.IIRFilter;
import com.ish.awtest2.func.KNNAlgorithm;
import com.ish.awtest2.func.LimitQueue;
import com.ish.awtest2.func.NKNNAlgorithm;
import com.ish.awtest2.func.Trainer;
import com.ish.awtest2.mview.TickView;

import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    private Double[] firstKnockx = new Double[ampLength];
    private Double[] firstKnocky = new Double[ampLength];
    private Double[] firstKnockz = new Double[ampLength];
    private Double[] finalData = new Double[finalLength];

    private NKNNAlgorithm nknnAlgorithm;
    /**
     * 训练数据n
     */
    Double[][] trainData;
    /**
     * 是否已经初始化数据
     */
    boolean ifIniData = false;

    private Double[] weight;

    private static final String TAG = "sensorTest";
    private String s = "";
    //
    private TickView tickView;
    private ImageView fingerImage;

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
//    private Runnable audioRunnable = new Runnable() {
//        @RequiresApi(api = Build.VERSION_CODES.M)
//        @Override
//        public void run() {
//            bufferResultLength = audioRecord.read(rawAudioData, 0, bufferSize, READ_NON_BLOCKING);
//            Log.d(TAG, "run: +bufferlength " + bufferResultLength);
//            handler.postDelayed(this, 1000);
//        }
//    };
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
     * 初始化view
     */
    public void iniView() {
        tickView = (TickView) findViewById(R.id.tick_view_test);
        fingerImage = (ImageView) findViewById(R.id.finger_image);
        btn = (Button) findViewById(R.id.test_btn);
        mTextViewCount = (TextView) findViewById(R.id.test_text_count);

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
                    //已经初始化数据
                    if (ifIniData) {
                        fingerImage.setVisibility(View.VISIBLE);
                        handler.postDelayed(runnable, 1000);
                        recLen = 0;
                        flag = true;
                        btn.setText("STOP");
                        btn.setAlpha(0);
                        tickView.setType(TickView.TYPE_ERROR);
                        tickView.setChecked(true);
                        btn.setClickable(false);
                    } else {
                        Toast.makeText(TestActivity.this, "waitting...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
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

                    if (!judgeshakeHands(datax) && !judgeshakeHands(datay) && !judgeshakeHands(dataz)) {
                        Double[] cutDatax = Cut.cutMoutain2(datax, 84, 35, 60, 1);
                        Double[] cutDatay = Cut.cutMoutain2(datay, 130, 35, 60, 2);
                        Double[] cutDataz = Cut.cutMoutain2(dataz, 84, 35, 60, 3);

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
//                        for (int i = 0; i < finalData.length; i++) {
//                            s = s + "," + finalData[i];
//                        }
//                        Log.d(TAG, "onSensorChanged: finalData" + s);
//                        s = "";
                        //将新的敲击数据加入对比
                        //double newDis = Trainer.getNewDis(trainData, finalData);
                        for (int i = 0; i < finalLength; i++) {
                            finalData[i] = finalData[i] * weight[i];
                        }
                        boolean isme = nknnAlgorithm.isMe(finalData);
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * 创建线程池，开启新的线程来初始化数据，避免页面打开卡顿延迟。
     */
    @Override
    protected void onStart() {
        super.onStart();
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        int KEEP_ALIVE_TIME = 1;
        TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        ExecutorService executorService = new ThreadPoolExecutor(NUMBER_OF_CORES,
                NUMBER_OF_CORES * 2,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                taskQueue);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                getTrainData();
            }
        });
        //getTrainData();
    }

    /**
     * 初始化数据，训练数据
     */
    private void getTrainData() {
        //取出训练数据
        String userName = getIntent().getStringExtra("userName");
        List<KnockData> allDatas = DataSupport.where("userName = ?", userName).find(KnockData.class);
        trainData = new Double[allDatas.size()][finalLength];
        int r = 0;
        for (KnockData row : allDatas) {
            trainData[r] = row.getArray();
            r++;
        }
        //取出weight
        List<StWeight> weightDatas = DataSupport.where("userName = ?", userName).find(StWeight.class);
        //如果没有weight,数据是原生的，就要std
        if (weightDatas.size() == 0) {
            weight = Trainer.calPower(trainData);
            for (int i = 0; i < trainData.length; i++) {
                trainData[i] = std(trainData[i], weight);
            }
        }
        //有weight，就不需要std
        else {
            Double[][] weightDataArray = new Double[weightDatas.size()][];
            r = 0;
            for (StWeight row : weightDatas) {
                weightDataArray[r] = row.getArray();
                r++;
            }
            //std
            weight = weightDataArray[0];
        }


        //取出阈值数据
        List<StThresholds> allThresholdDatas = DataSupport.where("userName = ?", userName)
                .find(StThresholds.class);
        double[][] allThreshold = new double[allThresholdDatas.size()][];
        r = 0;
        for (StThresholds row : allThresholdDatas) {
            allThreshold[r] = row.getArray();
            r++;
        }
        //取出label数据
        List<StLabel> labels = DataSupport.where("userName = ?", userName)
                .find(StLabel.class);
        int[][] label = new int[labels.size()][];
        r = 0;
        for (StLabel row : labels) {
            label[r] = row.getArray();
            r++;
        }

        //初始化第一个来对齐
        System.arraycopy(trainData[0], 0, firstKnockx, 0, ampLength);
        System.arraycopy(trainData[0], ampLength, firstKnocky, 0, ampLength);
        System.arraycopy(trainData[0], ampLength * 2, firstKnockz, 0, ampLength);

        //
        SharedPreferences p = getApplicationContext().getSharedPreferences("Myprefs",
                Context.MODE_PRIVATE);
        int range = 10;
        int value = 5;
        value = p.getInt("value", value);
        range = p.getInt("range", range);
        /**
         * 还没有训练过的数据
         */
        double[] newThreshold = new double[allThreshold.length];
        if (labels.size() == 0) {
            nknnAlgorithm = new NKNNAlgorithm(trainData);
            nknnAlgorithm.setLevel(value, range);
            nknnAlgorithm.generateKNNAlgorithm();
            //保存thresholds
            double[][] allThresholds = nknnAlgorithm.getAllThreshold();
            for (int i = 0; i < allThresholds.length; ++i) {
                StThresholds stThresholds = new StThresholds();
                stThresholds.initData(userName, allThresholds[i]);
                stThresholds.save();
            }
            //保存label
            int[] l = nknnAlgorithm.getLabel();
            StLabel stLabel = new StLabel();
            stLabel.initData(userName, l);
            stLabel.save();
        } else {
            //通过难度
            int stepNum = value - range / 2;
            for (int i = 0; i < allThreshold.length; ++i) {
                int index = allThreshold[i].length - (int) Math.ceil(allThreshold[i].length * 0.1);
                double stepSize = Math.abs(allThreshold[i][0] - allThreshold[i][allThreshold[i].length - 1])
                        / (allThreshold[i].length - 1);
                newThreshold[i] = allThreshold[i][index] - stepNum * stepSize;
            }
            Log.d(TAG, "getTrainData: labelsize" + label[0].length);
            nknnAlgorithm = new NKNNAlgorithm(trainData, label[0], newThreshold, value, range);
        }
//        if (nknnAlgorithm.hasOneSampleClass()) {
//        } else {
//        }
        ifIniData = true;
    }

    /**
     * 注册传感器监听器
     */
    @Override
    protected void onResume() {
        super.onResume();

        //创建一个SensorManager来获取系统的传感器服务
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                , 10000);
    }

    /**
     * 取消传感器监听器
     */
    @Override
    public void onPause() {
        sm.unregisterListener(this);
        super.onPause();
    }

    private boolean judgeshakeHands(Double[] array) {
        Double d = 0.22;
        boolean result = false;
        for (int i = 40; i < 80; ++i) {
            //有一个值大于阈值说明就是手晃动
            if (array[i] > d) {
                result = true;
            }
        }
        for (int i = 140; i < 170; ++i) {
            //有一个值大于阈值说明就是手晃动
            if (array[i] > d) {
                result = true;
            }
        }
        return result;
    }

    /**
     * std
     *
     * @param rowData
     * @param weight
     * @return
     */
    private Double[] std(Double[] rowData, Double[] weight) {
        Double[] result = new Double[rowData.length];
        for (int i = 0; i < rowData.length; i++) {
            result[i] = rowData[i] * weight[i];
        }
        return result;
    }
}
