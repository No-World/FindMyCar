package com.noworld.findmycar.ui;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.noworld.findmycar.R;

import java.io.File;

public class PictureActivity extends AppCompatActivity {

    private ImageView iv_picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_picture);

        initView();

        initPicture();
    }

    private void initView() {
        iv_picture = findViewById(R.id.iv_picture);
    }

    private void initPicture() {
        // 从/data/data/com.noworld.findmycar/files/car.png文件中加载图片
        File file = new File(getFilesDir(), "car.png");
        if (file.exists()) {
            iv_picture.setImageURI(Uri.fromFile(file));
        }
    }
}