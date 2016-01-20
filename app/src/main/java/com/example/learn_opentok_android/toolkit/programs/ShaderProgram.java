package com.example.learn_opentok_android.toolkit.programs;

import android.content.Context;
import android.opengl.GLES20;

import com.example.learn_opentok_android.toolkit.helpers.ShaderHelper;
import com.example.learn_opentok_android.toolkit.helpers.TextResourceReader;

/**
 * Created by quanhua on 19/01/2016.
 */
abstract class ShaderProgram {
    // Uniform constants
    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";
    protected static final String U_COLOR = "u_Color";
    protected static final String U_MVPMATRIX = "u_MVPMatrix";
    protected static final String U_STMATRIX = "u_STMatrix";
    protected static final String U_YTEX = "u_Ytex";
    protected static final String U_UTEX = "u_Utex";
    protected static final String U_VTEX = "u_Vtex";

    // Attribute constants
    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
        protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";

    // Shader program
    protected final int program;
    protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
        program = ShaderHelper.buildProgram(
                TextResourceReader.readTextFileFromResource(context, vertexShaderResourceId),
                TextResourceReader.readTextFileFromResource(context, fragmentShaderResourceId));
    }

    public void useProgram(){
        GLES20.glUseProgram(program);
    }
}