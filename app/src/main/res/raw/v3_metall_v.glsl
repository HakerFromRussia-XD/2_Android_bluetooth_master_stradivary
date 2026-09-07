//#version 120
uniform mat4 u_MVPMatrix;		// A constant representing the combined model/view/projection matrix.
uniform mat4 u_MVMatrix;		// A constant representing the combined model/view matrix.

attribute vec4 a_Position;		// Per-vertex position information we will pass in.   							
attribute vec3 a_Normal;		// Per-vertex normal information we will pass in.
attribute vec4 a_Color;			// Per-vertex color information we will pass in.
attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information we will pass in.
attribute vec3 a_TangentIn;
attribute vec3 a_BitangentIn;

varying vec3 v_Position;		// This will be passed into the fragment shader.       		          		
varying vec3 v_Normal;			// This will be passed into the fragment shader.
varying vec4 v_Color;			// This will be passed into the fragment shader.
varying vec2 v_TexCoordinate;   // This will be passed into the fragment shader.    		
varying mat3 v_TBNMatrix;

mat3 transpose(in mat3 inMatrix) {
    vec3 i0 = inMatrix[0];
    vec3 i1 = inMatrix[1];
    vec3 i2 = inMatrix[2];

    mat3 outMatrix = mat3 (
    vec3(i0.x, i1.x, i2.x),
    vec3(i0.y, i1.y, i2.y),
    vec3(i0.z, i1.z, i2.z)
    );

    return outMatrix;
}
// The entry point for our vertex shader.  
void main()
{
    // Transform the vertex into eye space.
    v_Position = vec3(u_MVMatrix * a_Position);

    // Pass through the color
    v_Color = a_Color;

    // Pass through the texture coordinate.
    v_TexCoordinate = a_TexCoordinate;

    // Transform the normal's orientation into eye space.
    v_Normal = normalize(vec3(u_MVMatrix * vec4(a_Normal, 0.0)));

    // gl_Position is a special variable used to store the final position.
    // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
    gl_Position = u_MVPMatrix * a_Position;

    vec3 tangent = normalize(vec4(u_MVMatrix * vec4(a_TangentIn, 0.0)).xyz);
    vec3 bitangent = normalize(vec4(u_MVMatrix * vec4(a_BitangentIn, 0.0)).xyz);
    vec3 normal = normalize(vec4(u_MVMatrix * vec4(a_Normal, 0.0)).xyz);
    v_TBNMatrix = transpose(mat3(tangent, bitangent, normal));
}                               