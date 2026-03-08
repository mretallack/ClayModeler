#version 300 es
precision mediump float;

in vec3 vNormal;
in vec3 vPosition;

uniform vec3 uLightPos;
uniform float uLightIntensity;
uniform vec3 uViewPos;
uniform vec3 uClayColor;

out vec4 fragColor;

void main() {
    // Normalize vectors
    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(uLightPos - vPosition);
    vec3 viewDir = normalize(uViewPos - vPosition);
    vec3 reflectDir = reflect(-lightDir, normal);
    
    // Ambient
    float ambientStrength = 0.3 * uLightIntensity;
    vec3 ambient = ambientStrength * uClayColor;
    
    // Diffuse
    float diff = max(dot(normal, lightDir), 0.0) * uLightIntensity;
    vec3 diffuse = diff * uClayColor;
    
    // Specular (matte finish - low specular)
    float specularStrength = 0.1 * uLightIntensity;
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 8.0);
    vec3 specular = specularStrength * spec * vec3(1.0, 1.0, 1.0);
    
    vec3 result = ambient + diffuse + specular;
    fragColor = vec4(result, 1.0);
}
