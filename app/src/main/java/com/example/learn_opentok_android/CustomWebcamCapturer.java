package com.example.learn_opentok_android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.opentok.android.BaseVideoCapturer;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by quanhua on 14/10/2015.
 */
public class CustomWebcamCapturer extends BaseVideoCapturer {
    private final static String LOGTAG = "customer-video-capturer";
    private Context context;

    private ReentrantLock mPreviewBufferLock = new ReentrantLock(); // sync
    // start/stop
    // capture
    // and
    // surface
    // changes

    private final static int PIXEL_FORMAT = ImageFormat.NV21;
    private final static int PREFERRED_CAPTURE_WIDTH = 640;
    private final static int PREFERRED_CAPTURE_HEIGHT = 480;

    private Long lastCaptureFrame = 0L;
    private Long currentCaptureFrame = 0L;

    private boolean isCaptureStarted = false;
    private boolean isCaptureRunning = false;

    private int mCaptureWidth = -1;
    private int mCaptureHeight = -1;
    private int mCaptureFPS = -1;

    private Display mCurrentDisplay;

    public CustomWebcamCapturer(Context context) {
        // Initialize front camera by default

        Log.d(LOGTAG, "CustomWebcamCapturer constructor");

        this.context = context;

        // Get current display to query UI orientation
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        mCurrentDisplay = windowManager.getDefaultDisplay();
    }

    public void addFrame(byte[] data){
        Log.d(LOGTAG, "addFrame");

        if(data == null){
            Log.d(LOGTAG, "hasFrame DATA NULL");
            return;
        }
        Log.d(LOGTAG, "hasFrame");
        mPreviewBufferLock.lock();
        imageData = data;
        mPreviewBufferLock.unlock();
    }

    @Override
    public int startCapture() {
        if (isCaptureStarted) {
            return -1;
        }

        Log.d(LOGTAG, "startCapture");

        mPreviewBufferLock.lock();
        isCaptureRunning = true;
        mPreviewBufferLock.unlock();

        isCaptureStarted = true;


        new Thread(new MyThread()).start();
        return 0;
    }

    private byte[] imageData = null;

    private class MyThread implements Runnable{
        @Override
        public void run() {
            while(true){
                mPreviewBufferLock.lock();
                if(imageData != null){
                    if (isCaptureRunning) {
                        Log.d(LOGTAG, "Has frame");
                        provideByteArrayFrame(imageData, NV21, mCaptureWidth,
                                mCaptureHeight, 0, false);
                    }
                    currentCaptureFrame = System.currentTimeMillis();
                }
                mPreviewBufferLock.unlock();

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public int stopCapture() {
        isCaptureStarted = false;
        return 0;
    }

    @Override
    public void destroy() {
        stopCapture();
    }

    @Override
    public boolean isCaptureStarted() {
        return isCaptureStarted;
    }

    @Override
    public CaptureSettings getCaptureSettings() {

        // Set the preferred capturing size
        configureCaptureSize(PREFERRED_CAPTURE_WIDTH, PREFERRED_CAPTURE_HEIGHT);

        CaptureSettings settings = new CaptureSettings();
        settings.fps = mCaptureFPS;
        settings.width = mCaptureWidth;
        settings.height = mCaptureHeight;
        settings.format = NV21;
        settings.expectedDelay = 0;
        return settings;
    }

    @Override
    public void onPause() {


    }

    @Override
    public void onResume() {

    }


    private void configureCaptureSize(int preferredWidth, int preferredHeight) {

        mCaptureFPS = 12;

        mCaptureWidth = preferredWidth;
        mCaptureHeight = preferredHeight;
    }

    @Override
    public void init() {

    }

}