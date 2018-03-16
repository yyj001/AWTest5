package com.ish.awtest2.func;

import android.util.Log;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by chenlin on 08/03/2018.
 */
public class KNNAlgorithm {
    private Double threshold = null;
    private double[] thresholds = null;
    private Double[][] data = null;

    /**
     * data 格式说明: data[i] 表示第 i 个样本
     * @param data 用户样本
     */
    public KNNAlgorithm(Double[][] data) {
        this.data = data;
        // 未传入阈值 说明样本未被训练过，需先进行训练以计算阈值
        train();
    }

    /**
     * data 格式说明: data[i] 表示第 i 个样本
     * @param data 用户样本
     * @param threshold 已计算好的阈值
     */
    public KNNAlgorithm(Double[][] data, double threshold) {
        this.data = data;
        this.threshold = threshold;
    }

    /**
     * 获取训练后计算的阈值
     * @return threshold
     */
    public double getThreshold() {
        return threshold;
    }

    public double[] getThresholds() {
        return thresholds;
    }

    /**
     * 自行设置阈值
     * @param threshold 新的阈值
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * 测试函数
     * @param testData 需要测试的样本
     * @return 本人返回 True (距离 <= 阈值) 非本人返回 False
     */
    public boolean isMe(Double[] testData) {
        return nearestDis(data, testData) <= threshold;
    }

    /**
     * 训练 用户样本，计算阈值，并将阈值保存到 threshold 中
     * 阈值计算方法：
     * 随机打乱样本，取 60% 作为训练样本 40% 作为测试样本
     * 默认取让测试样本通过 90% 时的距离作为阈值
     */
    private void train() {
        // 打乱样本
        shuffle(this.data);
        // 计算会用到的值
        int sampleSize = data.length;
        int trainSize = (int) Math.floor(sampleSize * 0.6);
        int testSize = sampleSize - trainSize;

        // k-fold
        int K = sampleSize / testSize;
        thresholds = new double[K * testSize];

        Double[][] trainData = new Double[trainSize][];
        Double[][] testData = new Double[testSize][];
        for (int k = 0; k < K; ++k) {
            int testCount = 0, trainCount = 0;
            // 将样本分成 训练样本 和 测试样本
            // 测试样本范围：[k * testSize, (k + 1) * testSize];
            for (int i = 0; i < sampleSize; ++i) {
                if (i >= k * testSize && i<(k+1)*testSize) {
                    testData[testCount++] = data[i];
                } else {
                    trainData[trainCount++] = data[i];
                }
            }

            // 计算距离
            for (int i = 0; i < testSize; ++i) {
                thresholds[k * testSize + i] = nearestDis(trainData, testData[i]);
            }
        }

        Arrays.sort(thresholds);
        int index = thresholds.length - (int) Math.ceil(thresholds.length * 0.1);
        this.threshold = thresholds[index];
    }

    private static void shuffle(Double[][] data) {
        Random rnd = new Random();
        for (int i = data.length; i > 1; i--) {
            Double[] tmp = data[i - 1];
            int index = rnd.nextInt(i-1);
            data[i - 1] = data[index];
            data[index] = tmp;
        }
    }

    /**
     * 计算离 testData 最近的训练样本的距离
     * @param trainData 训练样本
     * @param testData 测试样本
     * @return 离最近的训练样本的距离
     */
    private double nearestDis(Double[][] trainData, Double[] testData) {
        double minDis = Double.MAX_VALUE;
        for (int i = 0; i < trainData.length; ++i) {
            double tmp = Utils.calDis(trainData[i], testData);
            if (tmp < minDis) {
                minDis = tmp;
            }
        }
        return minDis;
    }

}
