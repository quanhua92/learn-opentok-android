uniform mat4 u_MVPMatrix;
attribute vec4 a_Position;
attribute vec2 a_TextureCoordinates;
varying vec2 vTextureCoord;
void main() {
    gl_Position = u_MVPMatrix * a_Position;
    vTextureCoord = a_TextureCoordinates;
}