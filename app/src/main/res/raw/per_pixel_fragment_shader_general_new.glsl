precision mediump float;       	// Set the default precision to medium. We don't need as high of a
// precision in the fragment shader.
uniform vec3 u_LightPos;       	// The position of the light in eye space.
uniform sampler2D u_Texture;    // The input texture.
uniform sampler2D u_normalMap;    // The input texture.
uniform int u_isUsingNormalMap;
uniform float u_specularFactor; // 30.0-metal      2.0   - plastic  1.0 - rubber
uniform float u_lightPower; //   3600.0-metal      900.0 - plastic/rubber
uniform float u_ambientFactor;


varying vec3 v_Position;		// Interpolated position for this fragment.
varying vec3 v_Normal;         	// Interpolated normal for this fragment.
varying vec4 v_Color;
varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.
varying mat3 v_TBNMatrix;

vec4 resultColor = vec4(0.0, 0.0, 0.0, 0.0);
vec3 eyePosition = vec3(0.0, 0.0, 150.5);
float ambientFactor = 0.8;

void main()
{
    vec4 diffMatColor = texture2D(u_Texture, v_TexCoordinate); //+
    vec3 usingNormal = v_Normal;
    if (u_isUsingNormalMap == 1) usingNormal =  normalize(normalize(texture2D(u_normalMap, v_TexCoordinate).rgb * 2.0  - 1.0 ) + (v_Normal * 2.0 ) );//* 2.0
    vec3 eyeVect = normalize(v_Position - eyePosition);
    //    if (u_isUsingNormalMap == 2) eyeVect = normalize(v_TBNMatrix * eyeVect);
    vec3 lightVector = normalize(u_LightPos - v_Position); //+
    //    if (u_isUsingNormalMap == 2) lightVector = normalize(v_TBNMatrix * lightVector);
    vec3 reflectLight = normalize(reflect(lightVector, usingNormal));
    float distance = length(u_LightPos - v_Position); //+
    float diffuse = max(dot(usingNormal, lightVector), 0.0); //+

    vec4 diffColor = diffMatColor * u_lightPower * diffuse / (1.0 + 0.25 * pow(distance, 2.0));//diffMatColor * lightPower * v_Color * diffuse /(1.0 + 0.25 + distance * distance);
    resultColor += diffColor;
    vec4 ambientColor = u_ambientFactor * diffMatColor;
    resultColor += ambientColor;
    vec4 specularColor = vec4(0.9, 0.9, 1.0, 1.0) * u_lightPower * pow(max(0.0, dot(reflectLight, eyeVect)), u_specularFactor) / (1.0 + 0.25 * pow(distance, 2.0));
    resultColor += specularColor;

    gl_FragColor = resultColor;
}

