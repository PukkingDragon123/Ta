#version 330
#extension GL_ARB_separate_shader_objects : require

// B1 Portal & sky FX: the gate's awakening. One triangle that covers the screen (3 vertices, no buffer).
layout(location = 0) out vec2 texCoord;

void main() {
    vec2 uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
    texCoord = uv;
}
