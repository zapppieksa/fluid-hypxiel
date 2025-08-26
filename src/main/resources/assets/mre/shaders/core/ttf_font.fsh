#version 150

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

uniform sampler2D Sampler0;
uniform float Smoothness;
uniform int Outline;
uniform vec4 OutlineColor;
uniform float OutlineThickness;

void main() {
    vec4 texColor = texture(Sampler0, texCoord);
    vec4 color = vertexColor * texColor;
    if (Outline == 1 && texColor.a > 0.0) {
        color = mix(color, OutlineColor, OutlineThickness);
    }
    color.a *= Smoothness; // Apply smoothness to alpha
    fragColor = color;
}