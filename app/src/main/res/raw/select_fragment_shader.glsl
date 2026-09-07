precision mediump float;

uniform float u_Code;
       					          
void main()                    
{
	gl_FragColor = vec4((u_Code / 255.0), 0.0, 0.0, 0.0);
}