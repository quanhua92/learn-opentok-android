package com.example.learn_opentok_android.toolkit;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.example.learn_opentok_android.toolkit.objects.Video;
import com.example.learn_opentok_android.toolkit.programs.VideoShaderProgram;
import com.opentok.android.BaseVideoRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by quanhua on 19/01/2016.
 */
public class GLRendererHelper implements GLSurfaceView.Renderer {
    int mTextureIds[] = new int[3];
    float[] mMVPMatrix = new float[16];

    private boolean mVideoFitEnabled = true;
    private boolean mVideoDisabled = false;

    static float mXYZCoords[] = {-1.0f, 1.0f, 0.0f, // top lef
            -1.0f, -1.0f, 0.0f, // bottom left
            1.0f, -1.0f, 0.0f, // bottom right
            1.0f, 1.0f, 0.0f // top right
    };

    static float mUVCoords[] = {0, 0, // top left
            0, 1, // bottom left
            1, 1, // bottom right
            1, 0}; // top right
    // vertices

    private Context context;
    private ReentrantLock mFrameLock = new ReentrantLock();
    private BaseVideoRenderer.Frame mCurrentFrame;


    // number of coordinates per vertex in this array
    protected float[] VERTEX_DATA = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 1.f,
            1.0f, -1.0f, 0, 1f, 1.f,
            -1.0f,  1.0f, 0, 0.f, 0.f,
            1.0f,  1.0f, 0, 1f, 0.f,
    };


    private int mViewportWidth;
    private int mViewportHeight;
    private int mTextureWidth;
    private int mTextureHeight;

    protected VideoShaderProgram videoShaderProgram;

    private Video video;

    public GLRendererHelper(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        gl10.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        videoShaderProgram = new VideoShaderProgram(
                context,
                VideoShaderProgram.DEFAULT_VERTEX_SHADER,
                VideoShaderProgram.DEFAULT_FRAGMENT_SHADER);

        video = new Video(VERTEX_DATA);
//        video = new Video(mXYZCoords, mUVCoords);

        mTextureWidth = 0;
        mTextureHeight = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mViewportWidth = width;
        mViewportHeight = height;
    }

    static void initializeTexture(int name, int id, int width, int height) {
        GLES20.glActiveTexture(name);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                width, height, 0, GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE, null);
    }
    void setupTextures(BaseVideoRenderer.Frame frame) {
        if (mTextureIds[0] != 0) {
            GLES20.glDeleteTextures(3, mTextureIds, 0);
        }
        GLES20.glGenTextures(3, mTextureIds, 0);

        int w = frame.getWidth();
        int h = frame.getHeight();
        int hw = (w + 1) >> 1;
        int hh = (h + 1) >> 1;

        initializeTexture(GLES20.GL_TEXTURE0, mTextureIds[0], w, h);
        initializeTexture(GLES20.GL_TEXTURE1, mTextureIds[1], hw, hh);
        initializeTexture(GLES20.GL_TEXTURE2, mTextureIds[2], hw, hh);

        mTextureWidth = frame.getWidth();
        mTextureHeight = frame.getHeight();
    }

    void updateTextures(BaseVideoRenderer.Frame frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        int half_width = (width + 1) >> 1;
        int half_height = (height + 1) >> 1;
        int y_size = width * height;
        int uv_size = half_width * half_height;

        ByteBuffer bb = frame.getBuffer();
        // If we are reusing this frame, make sure we reset position and
        // limit
        bb.clear();

        if (bb.remaining() == y_size + uv_size * 2) {
            bb.position(0);

            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
            GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[0]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
                    height, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                    bb);

            bb.position(y_size);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[1]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    half_width, half_height, GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE, bb);

            bb.position(y_size + uv_size);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[2]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    half_width, half_height, GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE, bb);
        } else {
            mTextureWidth = 0;
            mTextureHeight = 0;
        }

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        mFrameLock.lock();
        if (mCurrentFrame != null && !mVideoDisabled) {

            if (mTextureWidth != mCurrentFrame.getWidth()
                    || mTextureHeight != mCurrentFrame.getHeight()) {
                setupTextures(mCurrentFrame);
            }
            updateTextures(mCurrentFrame);

            updateMVPMatrix((float) mCurrentFrame.getWidth() , (float)mCurrentFrame.getHeight(), mCurrentFrame.isMirroredX());

            videoShaderProgram.useProgram();
            videoShaderProgram.setUniforms(mMVPMatrix);

            video.bindData(videoShaderProgram);
            video.draw();
        } else {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        }
        mFrameLock.unlock();
    }

    public void displayFrame(BaseVideoRenderer.Frame frame) {
        mFrameLock.lock();
        if (this.mCurrentFrame != null) {
            this.mCurrentFrame.recycle();
        }
        this.mCurrentFrame = frame;
        mFrameLock.unlock();
    }
    public void disableVideo(boolean b) {
        mFrameLock.lock();

        mVideoDisabled = b;

        if (mVideoDisabled) {
            if (this.mCurrentFrame != null) {
                this.mCurrentFrame.recycle();
            }
            this.mCurrentFrame = null;
        }

        mFrameLock.unlock();
    }

    public void enableVideoFit(boolean enableVideoFit) {
        mVideoFitEnabled = enableVideoFit;
    }

    public void updateMVPMatrix(float width, float height, boolean isMirroredX){
        Matrix.setIdentityM(mMVPMatrix, 0);
        float scaleX = 1.0f, scaleY = 1.0f;
        float ratio = width / height;
        float vratio = (float) mViewportWidth / mViewportHeight;

        if (mVideoFitEnabled) {
            if (ratio > vratio) {
                scaleY = vratio / ratio;
            } else {
                scaleX = ratio / vratio;
            }
        } else {
            if (ratio < vratio) {
                scaleY = vratio / ratio;
            } else {
                scaleX = ratio / vratio;
            }
        }

        Matrix.scaleM(mMVPMatrix, 0,
                scaleX * (isMirroredX ? -1.0f : 1.0f),
                scaleY, 1);
    }
}
