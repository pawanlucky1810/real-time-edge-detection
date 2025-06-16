package com.pawan.android;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.pawan.android.databinding.ActivityMainBinding;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ImageView imageView;

    private ImageTransformer imageTransformer = new ImageTransformer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        ImageView imageView = new ImageView(this);
        setContentView(imageView);

        Bitmap inputBitmap = loadImageFromAssets("sample.jpg");

        int width = inputBitmap.getWidth();
        int height = inputBitmap.getHeight();

        Bitmap bmp32 = inputBitmap.copy(Bitmap.Config.ARGB_8888, true);
        ByteBuffer buff = ByteBuffer.allocate(bmp32.getByteCount());

        bmp32.copyPixelsToBuffer(buff);

        byte[] resultData = imageTransformer.transformImage(buff.array(), width, height);

        Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        resultBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(resultData));

        imageView.setImageBitmap(resultBitmap);

    }

    private Bitmap loadImageFromAssets(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            return BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load image.", e);
        }
    }

}