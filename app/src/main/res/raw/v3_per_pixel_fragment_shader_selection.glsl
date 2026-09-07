precision mediump float;       	// Set the default precision to medium. We don't need as high of a
// precision in the fragment shader.
uniform vec3 u_LightPos;       	// The position of the light in eye space.
uniform sampler2D u_Texture;    // The input texture.
uniform sampler2D u_normalMap;    // The input texture.
uniform int u_isUsingNormalMap;
uniform float u_specularFactor; // 30.0-metal      2.0   - plastic  1.0 - rubber
uniform float u_lightPower; //   3600.0-metal      900.0 - plastic/rubber
uniform int u_FrontFaceMirrored;
uniform int u_UseSolidColor;
uniform vec4 u_SolidColor;
uniform int u_UseBlueSelection;

varying vec3 v_Position;		// Interpolated position for this fragment.
varying vec3 v_Normal;         	// Interpolated normal for this fragment.
varying vec4 v_Color;
varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.
varying mat3 v_TBNMatrix;

vec3 eyePosition = vec3(0.0, 0.0, 150.5);
float ambientFactor = 0.7;

// The entry point for our fragment shader.
void main()
{
    bool backFacing = u_FrontFaceMirrored == 1 ? gl_FrontFacing : !gl_FrontFacing;
    if (backFacing) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    float selectionMask = 1.0 - step(0.5, v_Color.b);
    bool useBlueSelection = u_UseBlueSelection == 1;
    vec3 selectionColor = useBlueSelection
        ? vec3(0.055, 0.690, 0.920)
        : v_Color.rgb;
    float selectionOpacity = useBlueSelection ? 0.05 : 1.0;
    vec3 selectionTint = mix(vec3(1.0), selectionColor, selectionOpacity);
    vec4 resultColor = vec4(
        0.2 * selectionMask * selectionColor * selectionOpacity,
        1.0
    );
    vec4 diffMatColor = u_UseSolidColor == 1
        ? u_SolidColor
        : texture2D(u_Texture, v_TexCoordinate); //+
    vec3 usingNormal = v_Normal;
    if (u_isUsingNormalMap == 1) usingNormal =  normalize(normalize(texture2D(u_normalMap, v_TexCoordinate).rgb * 2.0  - 1.0 ) + (v_Normal * 2.0 ) );//* 2.0
    vec3 eyeVect = normalize(v_Position.xyz - eyePosition.xyz);
    vec3 lightVector = normalize(u_LightPos - v_Position); //+
    vec3 reflectLight = normalize(reflect(lightVector, usingNormal));
    float distance = length(v_Position.xyz - eyePosition.xyz); //+
    float diffuse = max(0.0, dot(usingNormal, -lightVector)); //+

    vec4 diffColor = diffMatColor * u_lightPower * diffuse / (1.0 + 0.25 * pow(distance, 2.0));//diffMatColor * lightPower * v_Color * diffuse /(1.0 + 0.25 + distance * distance);
    resultColor += diffColor;
    vec4 ambientColor = ambientFactor * diffMatColor * vec4(selectionTint, 1.0);
    resultColor += ambientColor;
    vec4 specularColor = vec4(1.0, 1.0, 1.0, 1.0) * u_lightPower * pow(max(0.0, dot(reflectLight, eyeVect)), u_specularFactor) / (1.0 + 0.25 * pow(distance, 2.0));
    resultColor += specularColor;

    if (useBlueSelection) {
        float viewFacing = clamp(dot(normalize(usingNormal), -eyeVect), 0.0, 1.0);
        float fresnel = pow(1.0 - viewFacing, 2.2);
        float broadRim = smoothstep(0.08, 0.72, fresnel);
        float sharpRim = smoothstep(0.62, 0.94, fresnel);
        float technologyHighlight = 0.08 + 0.28 * broadRim + 0.46 * sharpRim;
        resultColor.rgb = mix(resultColor.rgb, selectionColor, technologyHighlight);
    }

    gl_FragColor = resultColor;
}
