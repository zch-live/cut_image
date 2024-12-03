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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

import cn.xz.cut_image.utils.CutImageUtils;

public class MainActivity extends AppCompatActivity {

    public static final int OPEN_GALLERY_REQUEST_CODE = 0;//本地相册

    ImageView output_Image;
    ImageView input_Image;
    TextView time_view;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
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

        CutImageUtils.setBgColor("#000000");
    }



    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, OPEN_GALLERY_REQUEST_CODE);
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
                        String[] proj = {MediaStore.Images.Media.DATA};
                        Cursor cursor = managedQuery(uri, proj, null, null, null);
                        cursor.moveToFirst();
                        CutImageUtils.onImageChanged(image, this, new CutImageUtils.CutImageInterface() {
                            @Override
                            public void back(Bitmap outputImage, Bitmap inputImage, String time) {
                                output_Image.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("手撕TAG", "手撕handleMessage: 消息接收识别成功-----" + time);
                                        input_Image.setImageBitmap(outputImage);
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
