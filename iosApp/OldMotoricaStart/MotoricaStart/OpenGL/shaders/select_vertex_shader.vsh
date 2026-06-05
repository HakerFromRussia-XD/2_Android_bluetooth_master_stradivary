uniform mat4 u_MVPMatrix;

#if __VERSION__ >= 140
in  vec4 inPosition;
#else
attribute vec4 inPosition;
#endif

void main()
{
    gl_Position = u_MVPMatrix * inPosition;
}   
