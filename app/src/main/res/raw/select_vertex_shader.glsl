uniform mat4 u_MVPMatrix;
uniform mat4 u_MVMatrix;

attribute vec4 a_Position;

void main()                                                 	
{
	gl_Position = u_MVPMatrix * a_Position;
}                                                          