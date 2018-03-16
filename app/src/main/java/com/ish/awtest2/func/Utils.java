package com.ish.awtest2.func;

/**
 * Created by chenlin on 08/03/2018.
 */
public class Utils {
    /**
     * 计算两两之间的欧式距离
     * @param data
     * @return res 返回 N * N 大小的矩阵
     *            res[i][j] 为第 i 个样本与第 j 个样本的距离
     */
    public static double[][] pdist(Double[][] data) {
        int len = data.length;
        double[][] res = new double[len][len];

        for (int i = 0; i < len; ++i) {
            res[i][i] = 0;
            for (int j = i + 1; j < len; ++j) {
                res[i][j] = res[j][i] = calDis(data[i], data[j]);
            }
        }

        return res;
    }

    /**
     * 计算两个样本的欧式距离
     * @param data1 第一个样本
     * @param data2 第二个样本
     * @return 两个样本的欧式距离
     */
    public static double calDis(Double[] data1, Double[] data2) {
        if (data1.length != data2.length) {
            return 0;
        }

        double dis = 0;
        for (int i = 0; i < data1.length; ++i) {
            dis += Math.pow(data1[i] - data2[i], 2);
        }
        return Math.sqrt(dis);
    }

    /**
     * 计算 data 样本的均值
     * @param data
     * @return 样本的均值
     */
    public static double mean(double[] data) {
        double res = 0;
        for (int i = 0; i < data.length; ++i) {
            res += data[i];
        }
        return res / data.length;
    }

    /**
     * 计算 data 样本的标准差
     * @param data
     * @return 样本的标准差
     */
    public static double std(double[] data) {
        double mean = mean(data);
        double res = 0;

        for (int i = 0; i < data.length; ++i) {
            res += Math.abs(data[i] - mean);
        }

        return res / data.length;
    }

    public static int max(int[] data) {
        int res = Integer.MIN_VALUE;
        for (int i = 0; i < data.length; ++i) {
            if (data[i] > res) {
                res = data[i];
            }
        }
        return res;
    }

}
