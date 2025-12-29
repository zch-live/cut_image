package cn.xz.cut_image.utils;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import cn.xz.cut_image.Predictor;
import cn.xz.cut_image.R;
import cn.xz.cut_image.Utils;
import cn.xz.cut_image.config.Config;
import cn.xz.cut_image.preprocess.Preprocess;
import cn.xz.cut_image.visual.Visualize;

public class CutImageUtils {

    //背景颜色
    private static String color = "#FFFFFF";
    //背景图片
    private static Bitmap backBitmap;
    //返回透明背景
    private static Boolean transparent = false;

    protected static Predictor predictor = new Predictor();

    protected static ProgressDialog pbRunModel = null;
    protected static ProgressDialog pbLoadModel = null;

    private static Context mContext;

    // 模型配置
    static Config config = new Config();

    static Preprocess preprocess = new Preprocess();

    static Visualize visualize = new Visualize();

    static CutImageInterface mCutImageInterface;

    public static void onImageChanged(Bitmap image, Context context,CutImageInterface cutImageInterface) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mCutImageInterface = cutImageInterface;
                setBitmap();
                Bitmap bg = backBitmap;
                Looper.prepare();
                setResume(context);
                // rerun model if users pick test image from gallery or camera
                //设置预测器图像
                if (image != null && predictor.isLoaded()) {
                    predictor.setInputImage(image, bg);
                    runModel();
                }
                Looper.loop();
            }
        }).start();
    }

    /**
     * 动态设置背景颜色，一定要在onImageChanged方法之前才有效果*/
    public static void setBgColor(String c){
        color = c;
    }

    /**
     * 设置返回透明背景，一定要在onImageChanged方法之前才有效果*/
    public static void setTransparent(){
        color = "#FFE4C4";
        transparent = true;
    }

    /**
     * 设置背景*/
    private static void setBitmap(){
        backBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(backBitmap);
        canvas.drawColor(Color.parseColor(color));//Color.RED
        canvas.drawBitmap(backBitmap, 0, 0, null);
    }

    /**
     * 初始化模型配置*/
    private static void setResume(Context context){
        mContext = context;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean settingsChanged = false;
        String model_path = sharedPreferences.getString(mContext.getString(R.string.MODEL_PATH_KEY), mContext.getString(R.string.MODEL_PATH_DEFAULT));
        String label_path = sharedPreferences.getString(mContext.getString(R.string.LABEL_PATH_KEY), mContext.getString(R.string.LABEL_PATH_DEFAULT));
        String image_path = sharedPreferences.getString(mContext.getString(R.string.IMAGE_PATH_KEY), mContext.getString(R.string.IMAGE_PATH_DEFAULT));
        String bg_path = sharedPreferences.getString(mContext.getString(R.string.BG_PATH_KEY), mContext.getString(R.string.BG_PATH_DEFAULT));
        settingsChanged |= !model_path.equalsIgnoreCase(config.modelPath);
        settingsChanged |= !label_path.equalsIgnoreCase(config.labelPath);
        settingsChanged |= !image_path.equalsIgnoreCase(config.imagePath);
        settingsChanged |= !bg_path.equalsIgnoreCase(config.bgPath);
        int cpu_thread_num = Integer.parseInt(sharedPreferences.getString(mContext.getString(R.string.CPU_THREAD_NUM_KEY), mContext.getString(R.string.CPU_THREAD_NUM_DEFAULT)));
        settingsChanged |= cpu_thread_num != config.cpuThreadNum;
        String cpu_power_mode = sharedPreferences.getString(mContext.getString(R.string.CPU_POWER_MODE_KEY), mContext.getString(R.string.CPU_POWER_MODE_DEFAULT));
        settingsChanged |= !cpu_power_mode.equalsIgnoreCase(config.cpuPowerMode);
        String input_color_format = sharedPreferences.getString(mContext.getString(R.string.INPUT_COLOR_FORMAT_KEY), mContext.getString(R.string.INPUT_COLOR_FORMAT_DEFAULT));
        settingsChanged |= !input_color_format.equalsIgnoreCase(config.inputColorFormat);
        long[] input_shape = Utils.parseLongsFromString(sharedPreferences.getString(mContext.getString(R.string.INPUT_SHAPE_KEY), mContext.getString(R.string.INPUT_SHAPE_DEFAULT)), ",");

        settingsChanged |= input_shape.length != config.inputShape.length;

        if (!settingsChanged) {
            for (int i = 0; i < input_shape.length; i++) {
                settingsChanged |= input_shape[i] != config.inputShape[i];
            }
        }

        if (settingsChanged) {
            config.init(model_path, label_path, image_path, bg_path, cpu_thread_num, cpu_power_mode, input_color_format, input_shape);
            preprocess.init(config);
            // 如果配置发生改变则重新加载模型并预测
            loadModel();
        }
    }



    public static void runModel() {
        pbRunModel = ProgressDialog.show(mContext, "", "推理中...", false, false);
        if (onRunModel()) {
            pbRunModel.dismiss();
            onRunModelSuccessed();
        } else {
            pbRunModel.dismiss();
            Toast.makeText(mContext, "Run model failed!", Toast.LENGTH_SHORT).show();
            onRunModelFailed();
        }
    }

    public static boolean onRunModel() {
        return predictor.isLoaded() && predictor.runModel(preprocess, visualize,transparent);
    }

    public static void loadModel() {
        pbLoadModel = ProgressDialog.show(mContext, "", "加载模型中...", false, false);
        if (onLoadModel()) {
            pbLoadModel.dismiss();
            onLoadModelSuccessed();
        } else {
            pbLoadModel.dismiss();
            Toast.makeText(mContext, "Load model failed!", Toast.LENGTH_SHORT).show();
            onLoadModelFailed();
        }
    }

    public static void onLoadModelSuccessed() {
        // load test image from file_paths and run model
        try {
            if (config.imagePath.isEmpty() || config.bgPath.isEmpty()) {
                return;
            }
            Bitmap image = null;
            Bitmap bg = null;

            //加载待抠图像（如果是拍照或者本地相册读取，则第一个字符为“/”。否则就是从默认路径下读取图片）
            if (!config.imagePath.substring(0, 1).equals("/")) {
                InputStream imageStream = mContext.getAssets().open(config.imagePath);
                image = BitmapFactory.decodeStream(imageStream);
            } else {
                if (!new File(config.imagePath).exists()) {
                    return;
                }
                image = BitmapFactory.decodeFile(config.imagePath);
            }
            bg = backBitmap;
            if (image != null && bg != null && predictor.isLoaded()) {
                predictor.setInputImage(image, bg);
                runModel();
            }
        } catch (IOException e) {
            Toast.makeText(mContext, "Load image failed!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public static boolean onLoadModel() {
        return predictor.init(mContext, config);
    }

    public static void onRunModelFailed() {
    }

    public static void onLoadModelFailed() {

    }

    public interface CutImageInterface{
        //outputImage - 识别后的图片
        //inputImage - 原图
        //time - 耗时(毫秒)
        void back(Bitmap outputImage,Bitmap inputImage,String time);
    }
    /**
     * 识别结束
     */
    @SuppressLint("ResourceAsColor")
    public static void onRunModelSuccessed() {
        // 获取抠图结果并更新UI
        Bitmap outputImage = predictor.outputImage();
        if (outputImage != null) {
            mCutImageInterface.back(outputImage,predictor.inputImage(),predictor.inferenceTime() + "");
        }
    }

    public static void destroy() {
        if (predictor != null) {
            predictor.releaseModel();
        }
    }
}
