#version 150

#moj_import <mre:common.glsl>

in vec3 Position; // POSITION_COLOR vertex attributes
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec4 FragColor;

void main() {
    FragCoord = rvertexcoord(gl_VertexID);
    FragColor = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}