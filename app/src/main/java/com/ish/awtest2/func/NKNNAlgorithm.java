package com.ish.awtest2.func;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by chenlin on 08/03/2018.
 */
public class NKNNAlgorithm {
    private Double[][] data;
    private int[] label;
    private double[] threshold;
    private double[][] allThreshold = null;
    private int classNum; // 聚类后的类的数量
    private KNNAlgorithm[] knnAlgorithms = null;
    //设置难度
    private int range;
    private int level;

    public NKNNAlgorithm(Double[][] data) {
        this.data = data;
        cluster();
    }

    public NKNNAlgorithm(Double[][] data, int level, int range) {
        this.data = data;
        this.level = level;
        this.range = range;
//        for (int i = 0; i < this.label.length; ++i) {
//            Log.d("aa", "NKNNAlgorithm: " + i + ":" + label[i]);
//        }
    }

    public NKNNAlgorithm(Double[][] data, int[] label, double[] threshold, int level, int range) {
        this.data = data;
        this.label = label;
        this.threshold = threshold;
        this.classNum = threshold.length;

        // 生成 KNNAlgorithm 对象
        knnAlgorithms = new KNNAlgorithm[classNum];
        for (int i = 0; i < classNum; ++i) {
            knnAlgorithms[i] = new KNNAlgorithm(getDataByClassNum(this.data, this.label, i + 1), threshold[i]);
        }
    }

    public boolean isMe(Double[] testData) {
        // 距离 testData 最近的样本所在的类
        int index = label[nearestDis(data, testData)];
        return knnAlgorithms[index - 1].isMe(testData);
    }

    /**
     * 第一次聚类在所有操作执行完成后调用该方法生成 KNNAlgorithm 对象
     * 注：执行完该方法后需要自行获取 data、label、threshold 信息进行更新数据库
     */
    public void generateKNNAlgorithm() {
        removeOneSampleClass(); // 移除 一类只有一个样本的类
        // 生成 KNNAlgorithm
        knnAlgorithms = new KNNAlgorithm[classNum];
        threshold = new double[classNum];
        allThreshold = new double[classNum][];
        for (int i = 0; i < classNum; ++i) {
            knnAlgorithms[i] = new KNNAlgorithm(getDataByClassNum(data, label, i + 1));
            threshold[i] = knnAlgorithms[i].getThreshold(level, range);
            allThreshold[i] = knnAlgorithms[i].getAllThreshold();
        }
    }

    /**
     * 获取n个类的threshold
     */
    public double[][] getAllThreshold() {
        return this.allThreshold;
    }

    /**
     * 判断是否有 一个类只有一个样本 的情况
     */
    public boolean hasOneSampleClass() {
        // 统计每一类的数量
        int[] num = getNumOfEachClass();

        for (int i = 0; i < classNum; ++i) {
            if (num[i] <= 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断加入新的样本后是否存在 一个类只有一个样本 的情况
     *
     * @param newData 用户新敲的样本
     */
    public boolean hasOneSampleClass(Double[][] newData) {
        data = mergeArray(data, newData);
        cluster();
        return hasOneSampleClass();
    }

    public Double[][] getData() {
        return data;
    }

    public int[] getLabel() {
        return label;
    }

    public void setLabel(int[] label) {
        this.label = label;
    }

    public void setLevel(int level, int range) {
        this.level = level;
        this.range = range;
    }

    public double[] getThreshold() {
        return threshold;
    }

    public void setThreshold(double[] threshold) {
        this.threshold = threshold;
    }

    public void setAllThreshold(double[][] threshold) {
        this.allThreshold = threshold;
    }

    /**
     * 删除一个类只有一个样本的类
     */
    private void removeOneSampleClass() {
        if (!hasOneSampleClass()) {
            return;
        }

        int[][] num = getSampleIndexOfEachClass();
        ArrayList<Double[]> newData = new ArrayList<>();
        ArrayList<Integer> newLabel = new ArrayList<>();

        classNum = 0;
        for (int i = 0; i < num.length; ++i) {
            if (num[i].length > 1) {
                ++classNum;
                for (int j = 0; j < num[i].length; ++j) {
                    newData.add(data[num[i][j]]);
                    newLabel.add(classNum);
                }
            }
        }

        data = new Double[newData.size()][];
        label = new int[newLabel.size()];
        for (int i = 0; i < newData.size(); ++i) {
            data[i] = newData.get(i);
            label[i] = newLabel.get(i);
        }
    }

    /**
     * 获取聚类后每一类的样本数量
     */
    private int[] getNumOfEachClass() {
        int[] num = new int[classNum];
        for (int i = 0; i < label.length; ++i) {
            ++num[label[i] - 1];
        }
        return num;
    }

    /**
     * 获取每一类的样本下标 index
     *
     * @return index[i][j] 表示第 i 类的第 j 个样本下标
     */
    private int[][] getSampleIndexOfEachClass() {
        int[][] res = new int[classNum][];
        int[] num = getNumOfEachClass();
        for (int i = 0; i < num.length; ++i) {
            res[i] = new int[num[i]];
        }
        int[] count = new int[classNum];
        for (int j = 0; j < label.length; ++j) {
            res[label[j] - 1][count[label[j] - 1]++] = j;
        }
        return res;
    }

    /**
     * 聚类
     */
    private void cluster() {
        int len = data.length;
        this.label = new int[len];
        int[] index = new int[len];

        double[] dis = getDisArray(index);

        // 粗聚类:先将一定为同一类(满足下列两个要求的其中之一)的聚类
        // 1. 低于阈值(平均距离 + 标准差)
        // 2. 比上一个距离小
        double threshold = Utils.mean(dis) + Utils.std(dis);
        classNum = 1;
        label[0] = 1;
        label[1] = 1;
        for (int i = 2; i < len; ++i) {
            if (dis[i] <= threshold || dis[i] < dis[i - 1]) {
                label[i] = label[i - 1];
            } else {
                ++classNum;
                label[i] = classNum;
            }
        }

        for (int i = 0; i < index.length; ++i) {
            Log.d("aa", "NKNNAlgorithm: " + i + ":" + index[i]);
        }

        // 恢复标签与 data 的对应关系
        int[] tmp = new int[len];
        for (int i = 0; i < len; ++i) {
            tmp[index[i]] = label[i];
        }
        label = tmp;

        // 合并相似类
        // 将样本数少的类向样本数多的类合并
        // 根据合并前后样本密度(两两之间距离的平均值)来决定是否进行合并
        boolean modified = true;
        while (modified) {
            modified = false;
            for (int i = 0; i < classNum; ++i) {
                Double[][] data1 = getDataByClassNum(data, label, i + 1);
                if (data1.length == 0) {
                    continue;
                }

                double minD = Double.MAX_VALUE;
                int minClassNum = 0;
                for (int j = 0; j < classNum; ++j) {
                    if (i == j) {
                        continue;
                    }

                    Double[][] data2 = getDataByClassNum(data, label, j + 1);
                    if (data2.length < data1.length) {
                        continue;
                    }

                    // 计算合并前的密度
                    double d1 = calDensity(data2);
                    // 计算合并后的密度
                    double d2 = calDensity(mergeArray(data1, data2));

                    if (d2 / d1 < minD) {
                        minD = d2 / d1;
                        minClassNum = j + 1;
                    }
                }

                // 比值小于 1.05 则进行合并
                if (minD < 1.05) {
                    modified = true;
                    for (int j = 0; j < len; ++j) {
                        if (label[j] - 1 == i) {
                            //Log.i("aa", String.valueOf(minClassNum));
                            label[j] = minClassNum;
                        }
                    }
                }
            }
        }

        // 恢复由于合并类导致的 label 不连续
        int maxClassNum = Utils.max(label);
        classNum = 0;
        for (int i = 0; i < maxClassNum; ++i) {
            for (int j = 0; j < len; ++j) {
                if (label[j] - 1 == i) {
                    ++classNum;
                    for (int k = 0; k < len; ++k) {
                        if (label[k] - 1 == i) {
                            label[k] = classNum;
                        }
                    }
                    break;
                }
            }
        }

    }

    /**
     * 计算样本的密度 (两两之间距离的平均值)
     *
     * @param data 样本
     * @return 密度
     */
    private double calDensity(Double[][] data) {
        double[][] dis = Utils.pdist(data);
        double sum = 0;
        int len = dis.length;
        for (int i = 0; i < len; ++i) {
            for (int j = i + 1; j < len; ++j) {
                sum += dis[i][j];
            }
        }
        return sum / (len * (len - 1) / 2);
    }

    private Double[][] mergeArray(Double[][] data1, Double[][] data2) {
        Double[][] newData = Arrays.copyOf(data1, data1.length + data2.length);
        System.arraycopy(data2, 0, newData, data1.length, data2.length);
        return newData;
    }

    /**
     * 从 data 从获取对应类编号的数据
     *
     * @param data
     * @param label    Data 对应的类编号
     * @param classNum 类的编号 从 1 开始
     * @return
     */
    private Double[][] getDataByClassNum(Double[][] data, int[] label, int classNum) {
        ArrayList<Double[]> res = new ArrayList<>();
        Log.d("a", "label " + label.length + " deatsize" + data.length);
        for (int i = 0; i < data.length; ++i) {
            if (label[i] == classNum) {
                res.add(data[i]);
            }
        }
        Double[][] result = new Double[res.size()][];
        for (int i = 0; i < res.size(); i++) {
            result[i] = res.get(i);
        }
//        return (Double[][]) res.toArray();
        return result;
    }

    /**
     * 聚类第一步：获取 dis 数组
     *
     * @param disIndex 保存计算 dis[i] 时的样本下标
     * @return dis 数组
     */
    private double[] getDisArray(int[] disIndex) {
        int len = data.length;

        double[] dis = new double[len]; // d[i] 表示找到第 i 个样本时的最小距离 d[0] = 0
        dis[0] = 0;

        boolean[] visited = new boolean[len]; // visited[i] 表示第 i 个样本是否已经被访问过
        for (int i = 0; i < len; ++i) {
            visited[i] = false;
        }

        double[][] pdis = Utils.pdist(data);

        // 先找到距离最小的两个样本下标
        int[] index = new int[2];
        double minDis = myFindMin(pdis, null, index);
        dis[1] = minDis;
        disIndex[0] = index[0];
        disIndex[1] = index[1];
        visited[index[0]] = visited[index[1]] = true;
        Log.d("1,2", "1 and 2: " + index[0] + " " + index[1]);
        // 接着遍历剩下的
        for (int i = 2; i < len; ++i) {
            minDis = myFindMin(pdis, visited, index);
//            if (visited[index[1]]) {
//                Log.e("aa", "error");
//            }
            visited[index[1]] = true;
            //Log.i("aa", String.valueOf(index[1]));
            disIndex[i] = index[1];
            dis[i] = minDis;
        }

        return dis;
    }

    /**
     * 查找 dis (两两之间距离数组) 的最小值
     * 由于是两两之间距离数组，因此对角线上为 0 的值不考虑
     * 寻找的最小值为一个样本在 visited 数组中标记为 true，另一个在 visited 数组中标记为 false
     * 当 visited 为 null 时没有上述限制
     *
     * @param dis     样本两两之间距离的数组
     * @param visited 标记某个样本是否被访问过的数组
     * @param index   保存最小距离的两个样本的下标. index[0] 为访问过的样本下标
     * @return 最小距离
     */
    private static double myFindMin(double[][] dis, boolean[] visited, int[] index) {
        int len = dis.length;
        double minDis = Double.MAX_VALUE;
        for (int i = 0; i < len; ++i) {
            // 先找到一个在 visited 数组中标记为 true (或 visited 为 null) 的样本
            if (visited != null && !visited[i]) {
                continue;
            }
            for (int j = 0; j < len; ++j) {
                // 再与在 visited 数组中标记为 false (或 visited 为 null) 的样本
                if ((visited == null || !visited[j]) && dis[i][j] < minDis && i != j) {
                    minDis = dis[i][j];
                    index[0] = i;
                    index[1] = j;
                }
            }
        }
        Log.d("pair", "pair i and j: " + index[0] + " " + index[1] + " " + minDis);
        if (minDis == Double.MAX_VALUE) {
            //Log.i("aa", "minDis = max");
            for (int i = 0; i < visited.length; ++i) {
                if (visited[i] == false) {
                    //Log.i("aa", "visited" + i + "=false");
                }
            }
        }
        return minDis;
    }

    /**
     * 计算离 testData 最近的训练样本的下标
     *
     * @param trainData 训练样本
     * @param testData  测试样本
     * @return 离最近的训练样本的下标
     */
    private int nearestDis(Double[][] trainData, Double[] testData) {
        double minDis = Double.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < trainData.length; ++i) {
            double tmp = Utils.calDis(trainData[i], testData);
            if (tmp < minDis) {
                minDis = tmp;
                index = i;
            }
        }
        return index;
    }

}
