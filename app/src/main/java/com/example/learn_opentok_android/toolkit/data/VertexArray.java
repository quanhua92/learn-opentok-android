package com.example.learn_opentok_android.toolkit.data;

import android.opengl.GLES20;

import com.example.learn_opentok_android.toolkit.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by quanhua on 19/01/2016.
 */
public class VertexArray {
    private final FloatBuffer floatBuffer;
//    private FloatBuffer mVertexBuffer;
//    private FloatBuffer mTextureBuffer;

    public VertexArray(float[] vertexData){
        floatBuffer = ByteBuffer.allocateDirect(vertexData.length * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
    }

//    public VertexArray(float[] mXYZCoords, float[] mUVCoords) {
//        ByteBuffer bb = ByteBuffer.allocateDirect(mXYZCoords.length * 4);
//        bb.order(ByteOrder.nativeOrder());
//        mVertexBuffer = bb.asFloatBuffer();
//        mVertexBuffer.put(mXYZCoords);
//        mVertexBuffer.position(0);
//
//        ByteBuffer tb = ByteBuffer.allocateDirect(mUVCoords.length * 4);
//        tb.order(ByteOrder.nativeOrder());
//        mTextureBuffer = tb.asFloatBuffer();
//        mTextureBuffer.put(mUVCoords);
//        mTextureBuffer.position(0);
//    }

    public void setVertexAttribPointer( int dataOffset, int attributeLocation, int componentCount, int stride){
        floatBuffer.position(dataOffset);
        GLES20.glVertexAttribPointer(attributeLocation, componentCount, GLES20.GL_FLOAT, false, stride, floatBuffer);
        GLES20.glEnableVertexAttribArray(attributeLocation);

        floatBuffer.position(0);
    }

//    public void setVertexAttribPointer( int attributeLocation, int componentCount, int stride, boolean isVertex){
//        if(isVertex){
//            mVertexBuffer.position(0);
//            GLES20.glVertexAttribPointer(attributeLocation, componentCount,
//                    GLES20.GL_FLOAT, false, stride,
//                    mVertexBuffer);
//        }else{
//            mTextureBuffer.position(0);
//            GLES20.glVertexAttribPointer(attributeLocation,
//                    componentCount, GLES20.GL_FLOAT, false,
//                    stride, mTextureBuffer);
//        }
//
//        GLES20.glEnableVertexAttribArray(attributeLocation);
//    }
}
