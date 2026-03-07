#version 300 es

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;
uniform mat4 uNormalMatrix;

out vec3 vNormal;
out vec3 vPosition;

void main() {
    vPosition = vec3(uModelMatrix * vec4(aPosition, 1.0));
    vNormal = mat3(uNormalMatrix) * aNormal;
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
}
