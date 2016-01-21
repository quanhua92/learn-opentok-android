package com.example.learn_opentok_android.toolkit.objects;

import android.opengl.GLES20;

import com.example.learn_opentok_android.toolkit.Constants;
import com.example.learn_opentok_android.toolkit.data.VertexArray;
import com.example.learn_opentok_android.toolkit.programs.VideoShaderProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Created by quanhua on 19/01/2016.
 */
public class Video {
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * Constants.BYTES_PER_FLOAT;

    private VertexArray vertexArray;

//    private ShortBuffer mDrawListBuffer;
//    private short mVertexIndex[] = {0, 1, 2, 0, 2, 3}; // order to draw
    public Video(float[] vertex){
        this.vertexArray = new VertexArray(vertex);
    }

//    public Video(float[] mXYZCoords, float[] mUVCoords) {
//
//        this.vertexArray = new VertexArray(mXYZCoords, mUVCoords);
//
//        ByteBuffer dlb = ByteBuffer.allocateDirect(mVertexIndex.length * 2);
//        dlb.order(ByteOrder.nativeOrder());
//        mDrawListBuffer = dlb.asShortBuffer();
//        mDrawListBuffer.put(mVertexIndex);
//        mDrawListBuffer.position(0);
//    }

    public void bindData(VideoShaderProgram videoShaderProgram){
        vertexArray.setVertexAttribPointer(
                0,
                videoShaderProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE
        );

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                videoShaderProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE
        );
//        vertexArray.setVertexAttribPointer(
//                videoShaderProgram.getPositionAttributeLocation(),
//                POSITION_COMPONENT_COUNT,
//                POSITION_COMPONENT_COUNT * 4,
//                true
//        );

//        vertexArray.setVertexAttribPointer(
//                videoShaderProgram.getTextureCoordinatesAttributeLocation(),
//                TEXTURE_COORDINATES_COMPONENT_COUNT,
//                TEXTURE_COORDINATES_COMPONENT_COUNT * 4,
//                false
//        );
    }

    public void draw(){
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mVertexIndex.length,
//                GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);
    }

    public void setVertexArray(float[] vertex){
        this.vertexArray = new VertexArray(vertex);
    }
}
