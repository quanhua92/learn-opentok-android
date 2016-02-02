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
    private Subscriber mSubscriber;

    private FrameLayout mPublisherViewContainer;
    private FrameLayout mSubscriberViewContainer;

    private CustomWebcamCapturer mCapturer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPublisherViewContainer = (FrameLayout)findViewById(R.id.publisher_container);
        mSubscriberViewContainer = (FrameLayout)findViewById(R.id.subscriber_container);


        mUVCCameraView = (SurfaceView) findViewById(R.id.camera_surface_view);
        mUVCCameraView.getHolder().addCallback(mSurfaceViewCallback);

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

        // TODO: Use a WEB SERVICE to get the sessionid, apikey, token
        mSessionId = ApiConfig.mSessionID;
        mToken = ApiConfig.mToken;
        mApiKey = ApiConfig.mApiKey;


        if (mSessionId.isEmpty()){
            Log.e(TAG, "Please Add Session, Token, ApiKey to ApiConfig.java");
            TextView tv = (TextView) findViewById(R.id.errorTextView);
            tv.setText("Please Add Session, Token, ApiKey to ApiConfig.java");
            Toast.makeText(MainActivity.this, "Please Add Session, Token, ApiKey to ApiConfig.java", Toast.LENGTH_LONG).show();
            return;
        }

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
        mPublisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisherViewContainer.addView(mPublisher.getView());
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

        if (mUVCCamera == null) {
            // XXX calling CameraDialog.showDialog is necessary at only first time(only when app has no permission).
            CameraDialog.showDialog(this);
        } else {
            synchronized (mSync) {
                mUVCCamera.destroy();
                mUVCCamera = null;
                isActive = isPreview = false;
            }
        }

        if (mPublisher != null) {
            mSession.publish(mPublisher);
        }
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(TAG, "Session Disconnected");

        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.destroy();
                mUVCCamera = null;
            }
            isActive = isPreview = false;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraView = null;
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(TAG, "Stream Received");

        if (mSubscriber == null) {
            mSubscriber = new Subscriber(this, stream);
            mSubscriber.setSubscriberListener(this);
            mSubscriber.setSubscribeToVideo(false);
            mSubscriber.setSubscribeToAudio(false);
            mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL);
            mSession.subscribe(mSubscriber);
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(TAG, "Stream Dropped");

        if (mSubscriber != null) {
            mSubscriber = null;
            mSubscriberViewContainer.removeAllViews();
        }
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

        mSubscriberViewContainer.addView(mSubscriber.getView());
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
        mUSBMonitor.register();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mSession.onPause();
        mSession.disconnect();
        mUSBMonitor.unregister();
    }


    // for thread pool
    private static final int CORE_POOL_SIZE = 1;		// initial/minimum threads
    private static final int MAX_POOL_SIZE = 4;			// maximum threads
    private static final int KEEP_ALIVE_TIME = 10;		// time periods while keep the idle thread
    protected static final ThreadPoolExecutor EXECUTER
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private final Object mSync = new Object();
    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private SurfaceView mUVCCameraView;
    // for open&start / stop&close camera preview
    private Surface mPreviewSurface;
    private boolean isActive, isPreview;

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(getApplicationContext(), "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            synchronized (mSync) {
                if (mUVCCamera != null)
                    mUVCCamera.destroy();
                isActive = isPreview = false;
            }
            EXECUTER.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (mSync) {
                        mUVCCamera = new UVCCamera();
                        mUVCCamera.open(ctrlBlock);

                        try {
                            mUVCCamera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
                        } catch (final IllegalArgumentException e) {
                            try {
                                // fallback to YUV mode
                                mUVCCamera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                            } catch (final IllegalArgumentException e1) {
                                mUVCCamera.destroy();
                                mUVCCamera = null;
                            }
                        }
                        if ((mUVCCamera != null) && (mPreviewSurface != null)) {
                            isActive = true;
                            mUVCCamera.setPreviewDisplay(mPreviewSurface);
                            mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);
                            mUVCCamera.startPreview();
                            isPreview = true;
                        }
                    }
                }
            });
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            // XXX you should check whether the comming device equal to camera device that currently using
            synchronized (mSync) {
                if (mUVCCamera != null) {
                    mUVCCamera.close();
                    if (mPreviewSurface != null) {
                        mPreviewSurface.release();
                        mPreviewSurface = null;
                    }
                    isActive = isPreview = false;
                }
            }
        }

        @Override
        public void onDettach(final UsbDevice device) {

            Toast.makeText(getApplicationContext(), "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel() {
        }
    };

    /**
     * to access from CameraDialog
     * @return
     */
    public USBMonitor getUSBMonitor() {

        Log.d(TAG, "getUSBMonitor");
        mCapturer = (CustomWebcamCapturer) mPublisher.getCapturer();
        mCapturer.addFrame(null);

        new Thread(new MyThread(mCapturer)).start();

        return mUSBMonitor;
    }

    class MyThread implements Runnable {
        private CustomWebcamCapturer mCapturer;
        private Long lastCameraTime = 0L;
        public MyThread(CustomWebcamCapturer cap) {
            mCapturer = cap;
        }

        @Override
        public void run() {
            while(true){

                byte[] capArray = null;
                imageArrayLock.lock();

                if(lastCameraTime != imageTime){
                    lastCameraTime = System.currentTimeMillis();
                    capArray = imageArray;
                }
                imageArrayLock.unlock();
                mCapturer.addFrame(capArray);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final SurfaceHolder.Callback mSurfaceViewCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
            if ((width == 0) || (height == 0)) return;

            mPreviewSurface = holder.getSurface();
            synchronized (mSync) {
                if (isActive && !isPreview) {
                    mUVCCamera.setPreviewDisplay(mPreviewSurface);
                    mUVCCamera.startPreview();
                    isPreview = true;
                }
            }
        }

        @Override
        public void surfaceDestroyed(final SurfaceHolder holder) {

            synchronized (mSync) {
                if (mUVCCamera != null) {
                    mUVCCamera.stopPreview();
                }
                isPreview = false;
            }
            mPreviewSurface = null;
        }
    };

    private byte[] imageArray = null;
    private Long imageTime = 0L;
    private ReentrantLock imageArrayLock = new ReentrantLock();
    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            imageArrayLock.lock();

            imageArray = new byte[frame.remaining()];
            frame.get(imageArray);

            if(imageArray == null){
                Log.d(TAG, "onFrame Lock NULL");
            }else{
//                Log.d(TAG, "onFrame Lock ");
            }

            imageTime = System.currentTimeMillis();
            imageArrayLock.unlock();
        }
    };
}
