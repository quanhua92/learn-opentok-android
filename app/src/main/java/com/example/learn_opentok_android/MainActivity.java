package com.example.learn_opentok_android;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.opentok.android.BaseVideoCapturer;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.OpentokError;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.UVCCameraTextureView;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener, SubscriberKit.SubscriberListener, CameraDialog.CameraDialogParent{

    public String TAG = MainActivity.class.getSimpleName();

    private String mApiKey;
    private String mSessionId;
    private String mToken;
    private Session mSession;


    private Publisher mPublisher;
//    private Subscriber mSubscriber;

    private FrameLayout mPublisherViewContainer;
    private FrameLayout mSubscriberViewContainer;

    private CustomWebcamCapturer mCapturer;

    private Thread streamingThread;

    /**
     * USB Camera Parameters
     */
    private USBCameraManager mUsbCameraManager = null;
    private UVCCameraTextureView mUVCCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Use a WEB SERVICE to get the sessionid, apikey, token
        mSessionId = ApiConfig.mSessionID;
        mToken = ApiConfig.mToken;
        mApiKey = ApiConfig.mApiKey;


        if (mSessionId.isEmpty()){
            Log.e(TAG, "Please Add Session, Token, ApiKey to ApiConfig.java");
            Toast.makeText(MainActivity.this, "Please Add Session, Token, ApiKey to ApiConfig.java", Toast.LENGTH_LONG).show();
            return;
        }

        findViewById(R.id.btnCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPublisher != null) {
                    mCapturer = (CustomWebcamCapturer) mPublisher.getCapturer();
                    mCapturer.addFrame(null);

                    streamingThread = new Thread(new MyThread(mCapturer));
                    streamingThread.start();

                    mSession.publish(mPublisher);
                    Toast.makeText(getApplicationContext(), "publish ok", Toast.LENGTH_SHORT).show();
                }
            }
        });
        initializeSession();
        initializePublisher();
    }

    private void initializeSession() {
        mSession = new Session(this, mApiKey, mSessionId);
        mSession.setSessionListener(this);
        mSession.connect(mToken);
    }

    private void initializePublisher() {
        mPublisher = new Publisher(this);
        mPublisher.setCapturer(new CustomWebcamCapturer(this));
        mPublisher.setPublishAudio(false);
        mPublisher.setPublisherListener(this);
//        mPublisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
//                BaseVideoRenderer.STYLE_VIDEO_FILL);
//        mPublisherViewContainer.addView(mPublisher.getView());
    }

    private void logOpenTokError(OpentokError opentokError) {
        Log.e(TAG, "Error Domain: " + opentokError.getErrorDomain().name());
        Log.e(TAG, "Error Code: " + opentokError.getErrorCode().name());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* Session Listener methods */

    @Override
    public void onConnected(Session session) {
        Log.i(TAG, "Session Connected");
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(TAG, "Session Disconnected");

        mUVCCameraView = null;
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(TAG, "Stream Received");
//
//        if (mSubscriber == null) {
//            mSubscriber = new Subscriber(this, stream);
//            mSubscriber.setSubscriberListener(this);
//            mSubscriber.setSubscribeToVideo(false);
//            mSubscriber.setSubscribeToAudio(false);
//            mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
//                    BaseVideoRenderer.STYLE_VIDEO_FILL);
//            mSession.subscribe(mSubscriber);
//        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(TAG, "Stream Dropped");
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        logOpenTokError(opentokError);
    }

    /* Publisher Listener methods */

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.i(TAG, "Publisher Stream Created");
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.i(TAG, "Publisher Stream Destroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        logOpenTokError(opentokError);
    }

    /* Subscriber Listener methods */

    @Override
    public void onConnected(SubscriberKit subscriberKit) {
        Log.i(TAG, "Subscriber Connected");

    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {
        Log.i(TAG, "Subscriber Disconnected");
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
        logOpenTokError(opentokError);
    }
    @Override
    public void onResume() {
        super.onResume();
        prepareCamera();
    }
    @Override
    protected void onPause() {
        super.onPause();

        if(!mUsbCameraManager.isRequestPermission()){
            Log.d(TAG, "onPause: isRequestPermission");
            releaseCamera();
        }

        if (streamingThread != null){
            streamingThread.interrupt();
        }

        mSession.onPause();
        mSession.disconnect();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUsbCameraManager.mUSBMonitor;
    }


    class MyThread implements Runnable {
        private CustomWebcamCapturer mCapturer;
        private Long lastCameraTimeLeft = 0L;
        private Long lastCameraTimeRight = 0L;
        public MyThread(CustomWebcamCapturer cap) {
            mCapturer = cap;
        }

        @Override
        public void run() {
            int old_width = UVCCamera.DEFAULT_PREVIEW_WIDTH, old_height = UVCCamera.DEFAULT_PREVIEW_HEIGHT;
            int new_width = old_width * 2;
            int new_height = old_height;

            byte[] data_new = new byte[new_width * new_height * 3 / 2];
            byte[] data_left = null;
            byte[] data_right = null;
            while(true){

                if(Thread.interrupted()){
                    Log.e(TAG, "Thread is interrupted");
                }

                imageLeftArrayLock.lock();

                if(lastCameraTimeLeft != imageLeftTime){
                    lastCameraTimeLeft = System.currentTimeMillis();
                    data_left = imageLeftArray;
                }
                imageLeftArrayLock.unlock();

                imageRightArrayLock.lock();

                if(lastCameraTimeRight != imageRightTime){
                    lastCameraTimeRight = System.currentTimeMillis();
                    data_right = imageRightArray;
                }
                imageRightArrayLock.unlock();


                mergeLeftRight(data_left, data_right, data_new, old_width, new_width, old_height);

                mCapturer.addFrame(data_new);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    /*
    Left Frame callback
     */
    private byte[] imageLeftArray = null;
    private Long imageLeftTime = 0L;
    private ReentrantLock imageLeftArrayLock = new ReentrantLock();

    private final IFrameCallback mIFrameLeftCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            imageLeftArrayLock.lock();

            imageLeftArray = new byte[frame.remaining()];
            frame.get(imageLeftArray);

            if(imageLeftArray == null){
                Log.d(TAG, "onFrame Lock NULL");
            }else{
//                Log.d(TAG, "onFrame Left Lock ");
            }


            ArrayUtils.reverse(imageLeftArray);
            // swap uv channel
            for (int i = 0; i < UVCCamera.DEFAULT_PREVIEW_WIDTH * UVCCamera.DEFAULT_PREVIEW_HEIGHT * 1 / 2; i += 2) {
                byte t = imageLeftArray[i];
                imageLeftArray[i] = imageLeftArray[i + 1];
                imageLeftArray[i + 1] = t;
            }

            imageLeftTime = System.currentTimeMillis();
            imageLeftArrayLock.unlock();
        }
    };
    /*
    Right Frame callback
     */
    private byte[] imageRightArray = null;
    private Long imageRightTime = 0L;
    private ReentrantLock imageRightArrayLock = new ReentrantLock();

    private final IFrameCallback mIFrameRightCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            imageRightArrayLock.lock();

            imageRightArray = new byte[frame.remaining()];
            frame.get(imageRightArray);

            if(imageRightArray == null){
                Log.d(TAG, "onFrame Lock NULL");
            }else{
//                Log.d(TAG, "onFrame Right Lock ");
            }

            ArrayUtils.reverse(imageRightArray);
            // swap uv channel
            for (int i = 0; i < UVCCamera.DEFAULT_PREVIEW_WIDTH * UVCCamera.DEFAULT_PREVIEW_HEIGHT * 1 / 2; i += 2) {
                byte t = imageRightArray[i];
                imageRightArray[i] = imageRightArray[i + 1];
                imageRightArray[i + 1] = t;
            }

            imageRightTime = System.currentTimeMillis();
            imageRightArrayLock.unlock();
        }
    };


    /**
     * Camera functions
     */
    private void prepareCamera(){
        Log.d(TAG, "prepareCamera");

        if(mUsbCameraManager == null){
            mUVCCameraView = (UVCCameraTextureView) findViewById(R.id.UVCCameraTextureView);

            Log.d(TAG, "prepareCamera: initialize & register UsbCameraManager");
            mUsbCameraManager = new USBCameraManager(this, detachCallback);
            mUsbCameraManager.setImageFormat(UVCCamera.PIXEL_FORMAT_NV21);
            mUsbCameraManager.setPreviewDisplay(mUVCCameraView);
            mUsbCameraManager.setLeftFrameCallback(mIFrameLeftCallback);
//            mUsbCameraManager.setLeftPreviewCallback(mIPreviewCallbackLeft);
//            mUsbCameraManager.toggleRightPreviewTexture(true);
            mUsbCameraManager.setRightFrameCallback(mIFrameRightCallback);
//            mUsbCameraManager.setRightPreviewCallback(mIPreviewCallbackRight);
            mUsbCameraManager.register();
        }
    }

    private void releaseCamera(){

        if(mUsbCameraManager != null){
            mUsbCameraManager.unregister();
            Log.d(TAG, "releaseCamera: unregister");
            mUsbCameraManager = null;
        }
    }

    private DetachCallback detachCallback = new DetachCallback() {
        @Override
        public void onDetach() {
            Log.e(TAG, "detach while recording");
        }
    };

    private void mergeLeftRight(byte[] data_left, byte[] data_right, byte[] nv21Data, int old_width, int new_width, int height) {

        if (data_left == null || data_right == null) return;

        /**
         * copy left data
         */
        int j = 0;
        int length = old_width;
        // get y channel
        for(int i = height * 1/2 ; i < height * 3/2; i ++){
            int srcPos = i * old_width;
            int destPos = j * new_width;
            System.arraycopy(data_left, srcPos, nv21Data, destPos, length);

            j++;
        }

        // get uv channel
        for(int i = 0 ; i < height * 1/2; i ++){
            int srcPos = i * old_width;
            int destPos = j * new_width;
            System.arraycopy(data_left, srcPos, nv21Data, destPos, length);

            j++;
        }

        /**
         * copy right data
         */
        // get y channel
        j = 0;
        for(int i = height * 1/2 ; i < height * 3/2; i ++){
            int srcPos = i * old_width;
            int destPos = j * new_width + old_width;
            System.arraycopy(data_right, srcPos, nv21Data, destPos, length);

            j++;
        }

        // get uv channel
        for(int i = 0 ; i < height * 1/2; i ++){
            int srcPos = i * old_width;
            int destPos = j * new_width + old_width;
            System.arraycopy(data_right, srcPos, nv21Data, destPos, length);

            j++;
        }
    }
}
