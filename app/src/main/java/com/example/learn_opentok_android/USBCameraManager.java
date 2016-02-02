package com.example.learn_opentok_android;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.UVCCameraTextureView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by quanhua on 03/11/2015.
 */
public class USBCameraManager {
    private String TAG = "USBCameraManager";
    private Context ctx;

    private Queue<UsbDevice> usbDeviceQueue = new LinkedList<>();

    private boolean LEFT_DISPLAY_ENABLED = false;
    private boolean isRequestingPermission = false;
    // for thread pool
    private static final int CORE_POOL_SIZE = 2;		// initial/minimum threads
    private static final int MAX_POOL_SIZE = 10;	    // maximum threads
    private static final int KEEP_ALIVE_TIME = 10;		// time periods while keep the idle thread
    protected static final ThreadPoolExecutor EXECUTER
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    public final Object mSync = new Object();
    // for accessing USB and USB camera
    public USBMonitor mUSBMonitor;

    private UVCCameraTextureView mUVCCameraView;

    public UVCCamera mCameraLeft = null;
    private SurfaceView mUVCCameraViewLeft;
    public Surface mPreviewSurfaceLeft;

    public UVCCamera mCameraRight = null;
    private SurfaceView mUVCCameraViewRight;
    public Surface mPreviewSurfaceRight;


    private SurfaceTexture mPreviewSurfaceTextureLeft, mPreviewSurfaceTextureRight;

    private IFrameCallback mIFrameCallbackLeft, mIFrameCallbackRight;
    private IFrameCallback mIPreviewCallbackLeft, mIPreviewCallbackRight;

    public boolean isActiveLeft, isPreviewLeft;
    public boolean isActiveRight, isPreviewRight;

    private LinkedBlockingQueue<Integer> queueDone = null;

    private DetachCallback callback;

    private int PIXEL_FORMAT = UVCCamera.PIXEL_FORMAT_RGBX;

    public USBCameraManager(final Context context, DetachCallback detachCallback) {

        callback = detachCallback;

        Log.e(TAG, "USBCameraManager constructor");
        queueDone = new LinkedBlockingQueue<>();

        ctx = context;
        mUSBMonitor = new USBMonitor(ctx, mOnDeviceConnectListener);

        mPreviewSurfaceTextureLeft = new SurfaceTexture(10);
        mPreviewSurfaceTextureRight = new SurfaceTexture(11);
    }

    public void autoRequestPermission(){
        List<UsbDevice> deviceList = mUSBMonitor.getDeviceList();
        Collections.reverse(deviceList);

        for(UsbDevice usbDevice : deviceList) {
            Log.e(TAG, "onAttach getVendorId: " + usbDevice.getVendorId());
            if ( usbDevice.getVendorId() != 3141 ) continue;

            if (usbDevice == null) {
                Log.e(TAG, "onAttach device is null");
            } else {
                Log.e(TAG, "onAttach " + usbDevice.getDeviceId() + " " + usbDevice.getDeviceName());

                if (MIN_ID == -1) {
                    MIN_ID = usbDevice.getDeviceId();
                } else {
                    MAX_ID = usbDevice.getDeviceId();
                    if (MIN_ID > MAX_ID) {
                        int t = MAX_ID;
                        MAX_ID = MIN_ID;
                        MIN_ID = t;
                    }
                }
            }

            if ( !usbDeviceQueue.contains(usbDevice) ){

                Log.e(TAG, "add to Queue: " + usbDevice.getDeviceId());
                usbDeviceQueue.add(usbDevice);

                isRequestingPermission = true;
            }
        }

        processQueueDevice();
    }

    public boolean isRequestPermission(){
        return isRequestingPermission;
    }
    private void processQueueDevice(){
        UsbDevice device = usbDeviceQueue.poll();

        if(device == null){
            Log.d(TAG, "Queue is empty");

            if(mCameraLeft != null){
                mCameraLeft.startPreview();
            }

            if(mCameraRight != null){
                mCameraRight.startPreview();
            }
            isRequestingPermission = false;
            return;
        }
        mUSBMonitor.requestPermission(device);
    }

    private int MIN_ID = -1, MAX_ID = -1;
    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(ctx, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();

            Log.e(TAG, "onAttach need autoRequest");
            autoRequestPermission();
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            final int debugID = device.getDeviceId();

            if(queueDone.isEmpty()){
                Log.e(TAG, "------queueDone is empty");
            }
            for(Object i : queueDone.toArray()){
                int v = ((Integer) i);
                Log.e(TAG, "-------- current in queueDone ID = " + v);
            }
            if(queueDone.contains(debugID)){
                Log.e(TAG, "already in QueueDone: " + debugID);
                processQueueDevice();
                return;
            }


            Log.e(TAG, "onConnect 0 : ID = " + debugID);

            if(mCameraLeft != null && mCameraRight != null) return;
            int SELECTED_ID = -1;
            if (device.getDeviceId() == MIN_ID){
                SELECTED_ID = 1;
            } else if (device.getDeviceId() == MAX_ID){
                SELECTED_ID = 0;
            }

            final int current_id = SELECTED_ID;


            Log.e(TAG, "onConnect 1 : ID = " + debugID);

            synchronized (mSync) {
                if(current_id == 0){
                    if (mCameraLeft != null)
                        mCameraLeft.destroy();
                    isActiveLeft = isPreviewLeft = false;
                }

                if(current_id == 1){
                    if (mCameraRight != null)
                        mCameraRight.destroy();
                    isActiveRight = isPreviewRight = false;
                }
            }

            Log.e(TAG, "onConnect 2 : ID = " + debugID);

            try {
                queueDone.put(debugID);
                Log.e(TAG, "onConnect 2 : put to queueDone ID = " + debugID);
                for(Object i : queueDone.toArray()){
                    int v = ((Integer) i);
                    Log.e(TAG, "onConnect 2 : current in queueDone ID = " + v);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            EXECUTER.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (mSync) {

                        if (current_id == 0) {

                            Log.e(TAG, "onConnect 3 : ID = " + debugID);

                            mCameraLeft = new UVCCamera();
                            mCameraLeft.open(ctrlBlock);

                            try {
                                Log.e(TAG, "onConnect 3b : setPreviewSize MJPEG");

                                mCameraLeft.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG, 0.6f);
                            } catch (final IllegalArgumentException e) {
                                try {
                                    // fallback to YUV mode
                                    Log.e(TAG, "onConnect 3b : setPreviewSize PREVIEW MODE" );
                                    mCameraLeft.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE, 0.6f);
                                } catch (final IllegalArgumentException e1) {
                                    mCameraLeft.destroy();
                                    mCameraLeft = null;
                                }
                            }

                            Log.e(TAG, "onConnect 4 : ID = " + debugID);

                            if ((mCameraLeft != null)) {
                                isActiveLeft = true;

                                if(mPreviewSurfaceTextureLeft != null){
                                    mCameraLeft.setPreviewTexture(mPreviewSurfaceTextureLeft);
                                } else {
                                    if(mPreviewSurfaceLeft != null){
                                        mCameraLeft.setPreviewDisplay(mPreviewSurfaceLeft);
                                    }
                                }

                                if(mUVCCameraView != null && LEFT_DISPLAY_ENABLED){
                                    mPreviewSurfaceLeft = new Surface(mUVCCameraView.getSurfaceTexture());
                                    mCameraLeft.setPreviewDisplay(mPreviewSurfaceLeft);
                                }

                                if(mIFrameCallbackLeft != null){
//                                    mCameraLeft.setFrameCallback(mIFrameCallbackLeft, UVCCamera.PIXEL_FORMAT_RGBX);
                                    mCameraLeft.setFrameCallback(mIFrameCallbackLeft, PIXEL_FORMAT);
                                }

                                Log.e(TAG, "onConnect 5 : ID = " + debugID);

//                                mCameraLeft.startPreview();
//                                isPreviewLeft = true;
                            }
                        }

                        if (current_id == 1) {

                            Log.e(TAG, "onConnect 3 : ID = " + debugID);

                            mCameraRight = new UVCCamera();
                            mCameraRight.open(ctrlBlock);

                            try {
                                Log.e(TAG, "onConnect 3b : setPreviewSize MJPEG" );
                                mCameraRight.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG, 0.5f);
                            } catch (final IllegalArgumentException e) {
                                try {
                                    // fallback to YUV mode
                                    Log.e(TAG, "onConnect 3b : setPreviewSize PREVIEW MODE" );
                                    mCameraRight.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE, 0.5f);
                                } catch (final IllegalArgumentException e1) {
                                    mCameraRight.destroy();
                                    mCameraRight = null;
                                }
                            }

                            Log.e(TAG, "onConnect 4 : ID = " + debugID);

                            if ((mCameraRight != null) ) {
                                isActiveRight = true;

                                if(mPreviewSurfaceTextureRight != null){
                                    mCameraRight.setPreviewTexture(mPreviewSurfaceTextureRight);
                                } else {
                                    if(mPreviewSurfaceRight != null){
                                        mCameraRight.setPreviewDisplay(mPreviewSurfaceRight);
                                    }
                                }

                                if(mIFrameCallbackRight != null){
                                    Log.e(TAG, "onConnect 4 : ID = " + debugID + " mIFrameCallbackRight != null");
//                                    mCameraRight.setFrameCallback(mIFrameCallbackRight, UVCCamera.PIXEL_FORMAT_RGBX);
                                    mCameraRight.setFrameCallback(mIFrameCallbackRight, PIXEL_FORMAT);
                                }else{
                                    Log.e(TAG, "onConnect 4 : ID = " + debugID + " mIFrameCallbackRight == null");
                                }

                                Log.e(TAG, "onConnect 5 : ID = " + debugID);
//                                mCameraRight.startPreview();
//                                isPreviewRight = true;
                            }
                        }

                        processQueueDevice();
                    }
                }
            });

        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            // XXX you should check whether the comming device equal to camera device that currently using
            synchronized (mSync) {

                // check queueDone
                if(queueDone.isEmpty()){
                    Log.e(TAG, "------onDisconnect queueDone is empty");
                }else{
                    for(Object i : queueDone.toArray()){
                        int v = ((Integer) i);
                        Log.e(TAG, "-------- onDisconnect current in queueDone ID = " + v);
                    }
                    queueDone.clear();
                    // check queueDone
                    if(queueDone.isEmpty()){
                        Log.e(TAG, "------onDisconnect queueDone is empty");
                    }
                }

                if (mCameraLeft != null && device.equals(mCameraLeft.getDevice())) {
                    releaseUVCCamera(0);
                }
                if(mCameraRight != null && device.equals(mCameraRight.getDevice())){
                    releaseUVCCamera(1);
                }
            }
        }

        @Override
        public void onDettach(final UsbDevice device) {
            callback.onDetach();
            Toast.makeText(ctx, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel() {
        }
    };
    private void releaseUVCCamera(int id){
        Log.v(TAG, "releaseUVCCamera");

        if(id == 0 || id == 2){
            if(mCameraLeft != null){
                mCameraLeft.close();
                if (mPreviewSurfaceLeft != null) {
                    mPreviewSurfaceLeft.release();
                    mPreviewSurfaceLeft = null;
                }
                isActiveLeft = isPreviewLeft = false;
                mCameraLeft.destroy();
                mCameraLeft = null;
            }
        }
        if(id == 1 || id == 2){
            if(mCameraRight != null){
                mCameraRight.close();
                if (mPreviewSurfaceRight != null) {
                    mPreviewSurfaceRight.release();
                    mPreviewSurfaceRight = null;
                }
                isActiveRight = isPreviewRight = false;
                mCameraRight.destroy();
                mCameraRight = null;
            }
        }
    }

    public void register() {
        mUSBMonitor.register();
    }

    public void setLeftSurfaceView(SurfaceView view) {
        this.mUVCCameraViewLeft = view;
        this.mUVCCameraViewLeft.getHolder().addCallback(mSurfaceViewLeftCallback);

        toggleLeftPreviewTexture(false);
    }
    public void setRightSurfaceView(SurfaceView view) {
        this.mUVCCameraViewRight = view;
        this.mUVCCameraViewRight.getHolder().addCallback(mSurfaceViewRightCallback);

        toggleRightPreviewTexture(false);
    }
    public void setLeftFrameCallback(IFrameCallback callback) {
        this.mIFrameCallbackLeft = callback;
    }
    public void setRightFrameCallback(IFrameCallback callback) {
        this.mIFrameCallbackRight = callback;
    }
    public void setLeftPreviewCallback(IFrameCallback callback) {
        this.mIPreviewCallbackLeft = callback;
    }
    public void setRightPreviewCallback(IFrameCallback callback) {
        this.mIPreviewCallbackRight = callback;
    }
    public void toggleLeftPreviewTexture(boolean enable) {
        if(enable){
            mPreviewSurfaceTextureLeft = new SurfaceTexture(10);
        }else{
            mPreviewSurfaceTextureLeft = null;
        }
    }
    public void toggleRightPreviewTexture(boolean enable) {
        if(enable){
            mPreviewSurfaceTextureRight = new SurfaceTexture(11);
        }else{
            mPreviewSurfaceTextureRight = null;
        }
    }

    public void unregister() {

        Log.d(TAG, "unregister");

        if (mCameraLeft != null){
            Log.d(TAG, "unregister: stopPreview Left");
            mCameraLeft.stopPreview();
            Log.d(TAG, "unregister: stopPreview Left Done");
        }
        if (mCameraRight != null){
            Log.d(TAG, "unregister: stopPreview Right");
            mCameraRight.stopPreview();
            Log.d(TAG, "unregister: stopPreview Right Done");
        }

        if (mCameraLeft != null){
            Log.d(TAG, "unregister: destroy Left");
            mCameraLeft.destroy();
        }

        if (mCameraRight != null){
            Log.d(TAG, "unregister: destroy Right");
            mCameraRight.destroy();
        }

        releaseUVCCamera(2);

        if(mUSBMonitor != null){
            Log.d(TAG, "unregister: mUsbMonitor.unregister");

            mUSBMonitor.unregister();
            mUSBMonitor.destroy();
        }
    }

    public void onSurfaceChanged(Surface surface, boolean isLeftCamera) {
        if(isLeftCamera){
            mPreviewSurfaceLeft = surface;
            synchronized (mSync) {
                if (isActiveLeft && !isPreviewLeft) {
                    mCameraLeft.setPreviewDisplay(mPreviewSurfaceLeft);
//                    mCameraLeft.startPreview();
                    isPreviewLeft = true;
                }
            }
        }else{
            mPreviewSurfaceRight = surface;
            synchronized (mSync) {
                if (isActiveRight && !isPreviewRight) {
                    mCameraRight.setPreviewDisplay(mPreviewSurfaceRight);
//                    mCameraRight.startPreview();
                    isPreviewRight = true;
                }
            }
        }

    }

    public void setPreviewDisplay(UVCCameraTextureView view){
        mUVCCameraView = view;
        LEFT_DISPLAY_ENABLED = true;
    }

    public void onSurfaceDestroy(boolean isLeftCamera) {
        if(isLeftCamera){
            synchronized (mSync) {
                if (mCameraLeft != null) {
                    mCameraLeft.stopPreview();
                }
                isPreviewLeft = false;
            }
            mPreviewSurfaceLeft = null;
        }else {
            synchronized (mSync) {
                if (mCameraRight != null) {
                    mCameraRight.stopPreview();
                }
                isPreviewRight = false;
            }
            mPreviewSurfaceRight = null;
        }
    }

    private final SurfaceHolder.Callback mSurfaceViewLeftCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
            if ((width == 0) || (height == 0)) return;

            onSurfaceChanged(holder.getSurface(), true);
        }

        @Override
        public void surfaceDestroyed(final SurfaceHolder holder) {
            onSurfaceDestroy(true);
        }
    };
    private final SurfaceHolder.Callback mSurfaceViewRightCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
            if ((width == 0) || (height == 0)) return;
            onSurfaceChanged(holder.getSurface(), false);
        }

        @Override
        public void surfaceDestroyed(final SurfaceHolder holder) {
            onSurfaceDestroy(false);
        }
    };

    public boolean isCameraReady() {
        return mCameraLeft != null && mCameraRight != null && isActiveLeft && isActiveRight;
    }

    public void setImageFormat(int pixelFormat) {
        PIXEL_FORMAT = pixelFormat;
    }
}
