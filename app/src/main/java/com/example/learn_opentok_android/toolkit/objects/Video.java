package com.example.learn_opentok_android.toolkit.objects;

import android.opengl.GLES20;

import com.example.learn_opentok_android.toolkit.Constants;
import com.example.learn_opentok_android.toolkit.data.VertexArray;
import com.example.learn_opentok_android.toolkit.programs.VideoShaderProgram;

/**
 * Created by quanhua on 19/01/2016.
 */
public class Video {
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * Constants.BYTES_PER_FLOAT;

    private VertexArray vertexArray;

    public Video(float[] vertex){
        this.vertexArray = new VertexArray(vertex);
    }

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
    }

    public void draw(){
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void setVertexArray(float[] vertex){
        this.vertexArray = new VertexArray(vertex);
    }
}
