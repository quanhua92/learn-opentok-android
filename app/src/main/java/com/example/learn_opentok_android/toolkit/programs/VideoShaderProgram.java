package com.example.learn_opentok_android.toolkit.programs;

import android.content.Context;
import android.opengl.GLES20;

/**
 * Created by quanhua on 19/01/2016.
 */
public class VideoShaderProgram extends ShaderProgram{
    public static final int DEFAULT_VERTEX_SHADER = com.example.learn_opentok_android.R.raw.default_vertex_shader;
    public static final int DEFAULT_FRAGMENT_SHADER = com.example.learn_opentok_android.R.raw.default_fragment_shader;

    // Uniform locations
    private int uMVPMatrixLocation;

    private int uYTexLocation;
    private int uUTexLocation;
    private int uVTexLocation;

    // Attribute locations
    private int aPositionLocation;
    private int aTextureCoordinatesLocation;

    public VideoShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
        super(context, vertexShaderResourceId, fragmentShaderResourceId);

        useProgram();

        aPositionLocation = GLES20.glGetAttribLocation(program, A_POSITION);
        aTextureCoordinatesLocation = GLES20.glGetAttribLocation(program, A_TEXTURE_COORDINATES);

        uMVPMatrixLocation = GLES20.glGetUniformLocation(program, U_MVPMATRIX);

        uYTexLocation = GLES20.glGetUniformLocation(program, U_YTEX);
        uUTexLocation = GLES20.glGetUniformLocation(program, U_UTEX);
        uVTexLocation = GLES20.glGetUniformLocation(program, U_VTEX);
    }

    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }

    public int getTextureCoordinatesAttributeLocation() {
        return aTextureCoordinatesLocation;
    }
    public void setUniforms(float[] mvpMatrix){
        GLES20.glUniformMatrix4fv(uMVPMatrixLocation, 1, false, mvpMatrix, 0 );
        GLES20.glUniform1i(uYTexLocation, 0); /* Bind Ytex to texture unit 0 */
        GLES20.glUniform1i(uUTexLocation, 1); /* Bind Utex to texture unit 1 */
        GLES20.glUniform1i(uVTexLocation, 2); /* Bind Vtex to texture unit 2 */
    }
}
