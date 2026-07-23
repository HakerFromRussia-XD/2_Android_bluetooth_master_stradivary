precision mediump float;       	// Set the default precision to medium. We don't need as high of a 
								// precision in the fragment shader.
uniform vec3 u_LightPos;       	// The position of the light in eye space.
uniform sampler2D u_Texture;    // The input texture.
  
varying vec3 v_Position;		// Interpolated position for this fragment.
varying vec3 v_Normal;         	// Interpolated normal for this fragment.
varying vec4 v_Color;
varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.

vec3 eyePosition = vec3(0.0, 0.0, 150.5);
vec4 resultColor = vec4(0.0, 0.0, 0.0, 0.0);
float specularFactor = 1.0;
float ambientFactor = 1.3;
float lightPower = 900.0;
// The entry point for our fragment shader.
void main()                    		
{
    vec4 diffMatColor = texture2D(u_Texture, v_TexCoordinate); //+
    vec3 eyeVect = normalize(v_Position - eyePosition);
    vec3 lightVector = normalize(u_LightPos - v_Position); //+
    vec3 reflectLight = normalize(reflect(lightVector, v_Normal));
    float distance = length(u_LightPos - v_Position); //+
    float diffuse = max(dot(v_Normal, lightVector), 0.0); //+

    vec4 diffColor = diffMatColor * lightPower * diffuse / (1.0 + 0.25 * pow(distance, 2.0));//diffMatColor * lightPower * v_Color * diffuse /(1.0 + 0.25 + distance * distance);
    resultColor += diffColor;
    vec4 ambientColor = ambientFactor * diffMatColor;
    resultColor += ambientColor;
    vec4 specularColor = vec4(1.0, 1.0, 1.0, 1.0) * lightPower * pow(max(0.0, dot(reflectLight, eyeVect)), specularFactor) / (1.0 + 0.25 * pow(distance, 2.0));
    resultColor += specularColor;

    gl_FragColor = resultColor;
  }                                                                     	

