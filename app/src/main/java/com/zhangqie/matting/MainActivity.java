package com.zhangqie.matting;


import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.xz.cut_image.utils.CutImageUtils;

public class MainActivity extends AppCompatActivity {

    public static final int OPEN_GALLERY_REQUEST_CODE = 0;//本地相册

    ImageView output_Image;
    ImageView input_Image;
    TextView time_view;
    Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // 禁用硬件加速
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        output_Image = findViewById(R.id.outputImage);
        input_Image = findViewById(R.id.inputImage);
        time_view = findViewById(R.id.time);
        //点击相册
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestAllPermissions()) {
                    openGallery();
                }
            }
        });
        findViewById(R.id.time).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveBitmapToAlbum(mBitmap);
            }
        });
        CutImageUtils.setTransparent();
    }



    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, OPEN_GALLERY_REQUEST_CODE);
    }


    /**
     * 保存bitmap到相册
     */
    private void saveBitmapToAlbum(Bitmap bitmap) {
        final File appDir = new File(this.getExternalCacheDir(), "image");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        final String fileName = System.currentTimeMillis() + "";
        final File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {

                // 使用 JPEG 格式保存图片
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            } finally {
                fos.close();
            }
            //把文件插入到系统相册
            MediaStore.Images.Media.insertImage(this.getContentResolver(), file.getAbsolutePath(), fileName, null);
            //通知图库更新
            //activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(file.getPath()))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if (resultCode == RESULT_OK && data != null) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case OPEN_GALLERY_REQUEST_CODE:
                    try {
                        ContentResolver resolver = getContentResolver();
                        Uri uri = data.getData();
                        Bitmap image = MediaStore.Images.Media.getBitmap(resolver, uri);
                        CutImageUtils.onImageChanged(image, this, new CutImageUtils.CutImageInterface() {
                            @Override
                            public void back(Bitmap outputImage, Bitmap inputImage, String time) {
                                output_Image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("手撕TAG", "手撕handleMessage: 消息接收识别成功-----" + time);
                                        mBitmap = outputImage;
                                        input_Image.setImageBitmap(mBitmap);
                                        output_Image.setImageBitmap(inputImage);
                                        time_view.setText("此次耗时" + time + "毫秒");
                                    }
                                });

                            }
                        });
                    } catch (IOException e) {
                        Log.e("TAG", e.toString());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private boolean requestAllPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA},
                    0);
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CutImageUtils.destroy();
    }




}
