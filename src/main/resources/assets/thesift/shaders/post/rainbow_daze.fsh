#version 330
#extension GL_ARB_separate_shader_objects : require

// A3 Chrome: the Rainbow Daze. The world swims gently, splits into soft rainbow fringes and its
// colours slowly turn round the colour wheel, strongest towards the edges of your sight.
uniform sampler2D InSampler;

// the game's global settings block (GlobalSettingsUniform, std140)
layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    float GlintAlpha;
    vec3 CameraOffset;
    float GameTime;
    vec2 ScreenSize;
    int MenuBlurRadius;
    int UseRgss;
};

layout(std140) uniform DazeConfig {
    float Strength;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;

vec3 hueRotate(vec3 c, float a) {
    // rotation about the grey axis: keeps brightness, turns the hue
    const vec3 k = vec3(0.57735);
    float ca = cos(a);
    return c * ca + cross(k, c) * sin(a) + k * dot(k, c) * (1.0 - ca);
}

void main() {
    // GameTime is the fraction of the day (24000 ticks); 1200 per second. Every rate below is a whole
    // number of cycles per day so nothing jumps when the day wraps.
    float t = GameTime * 1200.0;
    vec2 centred = texCoord - 0.5;
    float edge = smoothstep(0.08, 0.75, length(centred * vec2(ScreenSize.x / max(ScreenSize.y, 1.0), 1.0)));
    float s = Strength;
    // a slow, seasick swim of the whole image
    vec2 wave = vec2(sin(texCoord.y * 9.0 + t * TAU / 4.0), cos(texCoord.x * 7.0 + t * TAU / 5.0)) * 0.0045 * s;
    vec2 uv = texCoord + wave * (0.4 + edge);
    // rainbow fringes: red and blue pulled apart along the swirl
    vec2 split = vec2(cos(t * TAU / 6.0), sin(t * TAU / 6.0)) * (0.0015 + 0.0045 * edge) * s;
    vec3 c;
    c.r = texture(InSampler, uv + split).r;
    c.g = texture(InSampler, uv).g;
    c.b = texture(InSampler, uv - split).b;
    // the colours turn, more at the edges, in a band that sweeps across the screen
    float sweep = sin(dot(centred, vec2(3.0, 2.0)) * 3.0 - t * TAU / 3.0) * 0.5 + 0.5;
    c = mix(c, hueRotate(c, t * TAU / 6.0 + centred.x * 2.5), clamp(s * (0.35 + 0.45 * edge + 0.2 * sweep), 0.0, 1.0));
    // a little extra saturation for that candy-coloured haze
    float grey = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(grey), c, 1.0 + 0.35 * s);
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
