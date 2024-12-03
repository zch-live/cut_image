package cn.xz.cut_image.preprocess;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;

import android.graphics.Bitmap;
import android.util.Log;

import cn.xz.cut_image.config.Config;


public class Preprocess {

    private static final String TAG = Preprocess.class.getSimpleName();

    Config config;
    int channels;
    int width;
    int height;

    public  float[] inputData;

    public void init(Config config){
        this.config = config;
        this.channels = (int) config.inputShape[1];
        this.height = (int) config.inputShape[2];
        this.width = (int) config.inputShape[3];
        this.inputData = new float[channels * width * height];
    }

    public void normalize(float[] inputData){
        for (int i = 0; i < inputData.length; i++) {
            inputData[i] = (float) ((inputData[i] / 255 - 0.5) / 0.5);
        }
    }

    public boolean to_array(Bitmap inputImage){

        if (channels == 3) {
            int[] channelIdx = null;
            if (config.inputColorFormat.equalsIgnoreCase("RGB")) {
                channelIdx = new int[]{0, 1, 2};
            } else if (config.inputColorFormat.equalsIgnoreCase("BGR")) {
                channelIdx = new int[]{2, 1, 0};
            } else {
                Log.e("手撕TAG", "手撕handleMessage: 识别失败22222");
                return false;
            }
            int[] channelStride = new int[]{width * height, width * height * 2};

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = inputImage.getPixel(x, y);
                    float[] rgb = new float[]{(float) red(color) , (float) green(color) ,
                            (float) blue(color)};
                    inputData[y * width + x] = rgb[channelIdx[0]] ;
                    inputData[y * width + x + channelStride[0]] = rgb[channelIdx[1]] ;
                    inputData[y * width + x + channelStride[1]] = rgb[channelIdx[2]];
                }
            }
        } else if (channels == 1) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = inputImage.getPixel(x, y);
                    float gray = (float) (red(color) + green(color) + blue(color));
                    inputData[y * width + x] = gray;
                }
            }
        } else {
            Log.e("手撕TAG", "手撕handleMessage: 识别失败1111");
            return false;
        }
        return true;

    }

}
