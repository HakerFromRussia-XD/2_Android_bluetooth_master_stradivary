precision mediump float;       	// Set the default precision to medium. We don't need as high of a
// precision in the fragment shader.
uniform vec3 u_LightPos;       	// The position of the light in eye space.
uniform sampler2D u_Texture;    // The input texture.
uniform sampler2D u_normalMap;    // The input texture.
uniform int u_isUsingNormalMap;
uniform float u_specularFactor; // 30.0-metal      2.0   - plastic  1.0 - rubber
uniform float u_lightPower; //   3600.0-metal      900.0 - plastic/rubber
uniform float u_ambientFactor;
uniform int u_MaterialMode;
uniform float u_ChromeStrength;
uniform vec3 u_MetalFillLightDirection;
uniform vec3 u_MetalRimLightDirection;
uniform float u_MetalFillLightStrength;
uniform float u_MetalRimLightStrength;
uniform int u_FrontFaceMirrored;
uniform int u_UseSolidColor;
uniform vec4 u_SolidColor;


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
    bool backFacing = u_FrontFaceMirrored == 1 ? gl_FrontFacing : !gl_FrontFacing;
    if (backFacing) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    vec4 diffMatColor = u_UseSolidColor == 1
        ? u_SolidColor
        : texture2D(u_Texture, v_TexCoordinate); //+
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
    vec4 ambientColor = u_ambientFactor * diffMatColor;
    vec4 specularColor = vec4(0.9, 0.9, 1.0, 1.0) * u_lightPower * pow(max(0.0, dot(reflectLight, eyeVect)), u_specularFactor) / (1.0 + 0.25 * pow(distance, 2.0));

    if (u_MaterialMode == 1 || u_specularFactor > 20.0) {
        vec3 viewToEye = normalize(eyePosition - v_Position);
        vec3 chromeNormal = -normalize(usingNormal);
        vec3 reflectedView = normalize(reflect(-viewToEye, chromeNormal));
        float metalDistance = length(v_Position - eyePosition);
        float metalDiffuse = max(0.0, dot(chromeNormal, -lightVector));
        vec3 metalReflectLight = normalize(reflect(lightVector, chromeNormal));
        vec3 metalDiffColor = (diffMatColor * u_lightPower * metalDiffuse / (1.0 + 0.25 * pow(metalDistance, 2.0))).rgb;
        vec3 metalAmbientColor = diffMatColor.rgb * 0.46;
        vec3 metalSpecularColor = vec3(1.0, 1.0, 1.0) * u_lightPower * pow(max(0.0, dot(metalReflectLight, eyeVect)), u_specularFactor) / (1.0 + 0.25 * pow(metalDistance, 2.0));
        float fresnel = pow(1.0 - max(0.0, dot(chromeNormal, viewToEye)), 2.2);

        vec3 fillLightDirection = normalize(u_MetalFillLightDirection);
        vec3 fillReflect = normalize(reflect(fillLightDirection, chromeNormal));
        float fillDiffuse = max(0.0, dot(chromeNormal, -fillLightDirection));
        float fillSpecular = pow(max(0.0, dot(fillReflect, viewToEye)), 18.0);
        vec3 rimLightDirection = normalize(u_MetalRimLightDirection);
        vec3 rimReflect = normalize(reflect(rimLightDirection, chromeNormal));
        float rimDiffuse = max(0.0, dot(chromeNormal, -rimLightDirection));
        float rimSpecular = pow(max(0.0, dot(rimReflect, viewToEye)), 26.0);
        float rimEdge = pow(fresnel, 1.15) * rimDiffuse;

        vec3 coolBandDirection = normalize(vec3(-0.60, 0.36, 0.72));
        vec3 whiteBandDirection = normalize(vec3(0.48, -0.58, 0.66));
        vec3 shadowBandDirection = normalize(vec3(0.12, 0.88, 0.46));
        vec3 broadShadowDirection = normalize(vec3(-0.34, -0.72, 0.60));
        float coolBand = pow(max(0.0, dot(chromeNormal, coolBandDirection)), 4.0);
        float whiteBand = pow(max(0.0, dot(chromeNormal, whiteBandDirection)), 13.0);
        float shadowBand = pow(max(0.0, dot(chromeNormal, shadowBandDirection)), 2.6);
        float broadShadow = pow(max(0.0, dot(chromeNormal, broadShadowDirection)), 1.8);
        float metalShadow = clamp(shadowBand * 1.26 + broadShadow * 0.84, 0.0, 0.82);

        vec3 leftPanelDirection = normalize(vec3(-0.92, 0.12, 0.38));
        vec3 rightPanelDirection = normalize(vec3(0.86, 0.18, 0.48));
        vec3 topPanelDirection = normalize(vec3(0.04, 0.94, 0.34));
        vec3 lowerDarkDirection = normalize(vec3(0.14, -0.78, 0.62));
        vec3 sideDarkDirection = normalize(vec3(-0.72, -0.18, 0.67));
        float leftPanel = pow(max(0.0, dot(reflectedView, leftPanelDirection)), 7.0);
        float rightPanel = pow(max(0.0, dot(reflectedView, rightPanelDirection)), 7.0);
        float topPanel = pow(max(0.0, dot(reflectedView, topPanelDirection)), 4.2);
        float longPanel = pow(max(0.0, 1.0 - abs(dot(reflectedView, normalize(vec3(0.58, 0.0, 0.82))))), 4.0);
        float lowerDark = pow(max(0.0, dot(reflectedView, lowerDarkDirection)), 2.0);
        float sideDark = pow(max(0.0, dot(reflectedView, sideDarkDirection)), 2.4);
        float skyGradient = clamp(reflectedView.y * 0.55 + 0.50, 0.0, 1.0);
        float sideGradient = clamp(abs(reflectedView.x) * 0.56 + reflectedView.z * 0.18, 0.0, 1.0);
        float panelLight = leftPanel * 0.42 + rightPanel * 0.36 + topPanel * 0.34 + longPanel * 0.18;
        float reflectedShadow = clamp(lowerDark * 0.42 + sideDark * 0.34, 0.0, 0.72);

        float formLight = clamp(metalDiffuse * 0.58
                + fillDiffuse * u_MetalFillLightStrength * 0.28
                + rimDiffuse * u_MetalRimLightStrength * 0.18
                + panelLight * 0.34
                + fresnel * 0.16, 0.0, 1.0);
        float formShadow = pow(1.0 - formLight, 1.35);

        vec3 environmentColor = mix(vec3(0.26, 0.28, 0.31), vec3(0.64, 0.72, 0.84), skyGradient);
        environmentColor += vec3(0.42, 0.50, 0.62) * sideGradient * 0.22;

        float facing = max(0.0, dot(chromeNormal, viewToEye));
        float edgeShadow = pow(1.0 - facing, 1.15);
        float normalSideShadow = pow(abs(chromeNormal.x), 1.35) * 0.28;
        float normalLowerShadow = pow(max(0.0, -chromeNormal.y), 1.35) * 0.34;
        float broadMainHighlight = pow(metalDiffuse, 2.8);
        float broadFillHighlight = pow(fillDiffuse, 2.2);
        float metalOcclusion = clamp(edgeShadow * 0.58
                + normalSideShadow
                + normalLowerShadow
                + metalShadow * 0.30
                + reflectedShadow * 0.34, 0.0, 0.56);
        float metalLight = clamp(metalDiffuse * 0.72
                + fillDiffuse * u_MetalFillLightStrength * 0.22
                + rimDiffuse * u_MetalRimLightStrength * 0.13
                + facing * 0.26
                + panelLight * 0.18, 0.0, 1.0);

        vec3 metalBaseColor = max(diffMatColor.rgb, vec3(0.38, 0.40, 0.41));
        vec3 metalShadowFloor = metalBaseColor * (0.48 + metalLight * 0.12);

        vec3 chromeColor = metalBaseColor * (0.34 + metalLight * 0.54);
        chromeColor += metalDiffColor * 0.30;
        chromeColor += metalAmbientColor * 0.20;
        chromeColor += environmentColor * 0.18;
        chromeColor *= 1.0 - metalOcclusion;
        chromeColor = max(chromeColor, metalShadowFloor);
        chromeColor += metalSpecularColor * 1.10;
        chromeColor += vec3(0.88, 0.94, 1.0) * broadMainHighlight * 0.18;
        chromeColor += vec3(0.76, 0.84, 1.0) * broadFillHighlight * u_MetalFillLightStrength * 0.12;
        chromeColor += vec3(0.84, 0.91, 1.0) * panelLight * u_ChromeStrength * 0.32;
        chromeColor += vec3(0.88, 0.94, 1.0) * fillSpecular * u_MetalFillLightStrength * u_ChromeStrength * 0.38;
        chromeColor += vec3(0.82, 0.90, 1.0) * rimSpecular * u_MetalRimLightStrength * u_ChromeStrength * 0.50;
        chromeColor += vec3(0.62, 0.76, 1.0) * rimEdge * u_MetalRimLightStrength * u_ChromeStrength * 0.26;
        chromeColor += vec3(0.80, 0.88, 1.0) * coolBand * u_ChromeStrength * 0.12;
        chromeColor += vec3(1.0, 1.0, 0.94) * whiteBand * u_ChromeStrength * 0.26;
        chromeColor += vec3(0.78, 0.88, 1.0) * fresnel * u_ChromeStrength * 0.15;

        gl_FragColor = vec4(clamp(chromeColor, vec3(0.0), vec3(1.0)), diffMatColor.a);
    } else {
        resultColor += diffColor;
        resultColor += ambientColor;
        resultColor += specularColor;

        gl_FragColor = resultColor;
    }
}
