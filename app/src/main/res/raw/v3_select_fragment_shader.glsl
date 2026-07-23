precision mediump float;

uniform float u_Code;
varying vec4 v_Color;

void main()
{
	float redCode = u_Code > 0.5 ? (u_Code / 255.0) : v_Color.r;
	gl_FragColor = vec4(redCode, 0.0, 0.0, 1.0);
}
