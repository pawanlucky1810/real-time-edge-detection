package com.pawan.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.pawan.android.databinding.ActivityMainBinding;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;

public class MainActivity extends
        AppCompatActivity {

    private ActivityMainBinding binding;
    private ImageTransformer imageTransformer = new ImageTransformer();

    private TextureView textureView;
    private Switch toggleEdge;
    private TextView fpsText;
    private boolean showEdges = false;

    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private long lastFrameTime;

    private ImageView processedImageView;

    //    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//
//        ImageView imageView = new ImageView(this);
//        setContentView(imageView);
//
//        Bitmap inputBitmap = loadImageFromAssets("sample.jpg");
//
//        int width = inputBitmap.getWidth();
//        int height = inputBitmap.getHeight();
//
//        Bitmap bmp32 = inputBitmap.copy(Bitmap.Config.ARGB_8888, true);
//        ByteBuffer buff = ByteBuffer.allocate(bmp32.getByteCount());
//
//        bmp32.copyPixelsToBuffer(buff);
//
//        byte[] resultData = imageProcessor.transformImage(buff.array(), width, height);
//
//        Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//
//        resultBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(resultData));
//
//        imageView.setImageBitmap(resultBitmap);
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
        toggleEdge = findViewById(R.id.toggleEdges);
        fpsText = findViewById(R.id.fpsText);
        // processedImageView = findViewById(R.id.processingImageView);
        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);
        glRenderer = new GLRenderer(this);
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        toggleEdge.setOnCheckedChangeListener((buttonView, isChecked) -> showEdges = isChecked);

        textureView.setSurfaceTextureListener(surfaceTextureListener);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            long currentTime = System.nanoTime();
            if (lastFrameTime > 0) {
                double fps = 1e9 / (currentTime - lastFrameTime);
                runOnUiThread(() -> {
                    fpsText.setText(String.format("FPS: %.1f", fps));
                });
            }
            lastFrameTime = currentTime;

            if(!showEdges) {
                glSurfaceView.setVisibility(View.GONE);
                return;
            }
            if (showEdges) {
                Bitmap inputBitmap = textureView.getBitmap();
                if (inputBitmap == null) return;

                int width = inputBitmap.getWidth();
                int height = inputBitmap.getHeight();

                Bitmap bmp32 = inputBitmap.copy(Bitmap.Config.ARGB_8888, true);
                ByteBuffer buff = ByteBuffer.allocate(bmp32.getByteCount());
                bmp32.copyPixelsToBuffer(buff);
                byte[] resultData = imageTransformer.transformImage(buff.array(), width, height);
                Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                resultBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(resultData));
                runOnUiThread(() -> {
                    glRenderer.setBitmap(resultBitmap);
                    glSurfaceView.requestRender();
                    glSurfaceView.setVisibility(View.VISIBLE);
//                    processedImageView.setImageBitmap(resultBitmap);
//                    processedImageView.setVisibility(View.VISIBLE);
                });
            }
        }
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                }
            }, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            Surface surface = new Surface(texture);

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        super.onPause();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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