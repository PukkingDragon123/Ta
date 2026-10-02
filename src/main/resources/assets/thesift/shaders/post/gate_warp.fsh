#version 330
#extension GL_ARB_separate_shader_objects : require

// B1 Portal & sky FX: the gate's awakening. The camera faces the gate, so the gate is the middle of
// the screen: the world twists and is drawn in towards it, the air shimmers like heat, colours split
// and turn cyan as the light gathers. After the flash a ring of bent light rolls out to the edges.
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

layout(std140) uniform WarpConfig {
    float Strength;
    float Shock;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;

void main() {
    // GameTime is the fraction of the day; x1200 gives seconds. Every period divides 1200 s.
    float t = GameTime * 1200.0;
    float aspect = ScreenSize.x / max(ScreenSize.y, 1.0);
    vec2 p = (texCoord - 0.5) * vec2(aspect, 1.0);
    float r = length(p);
    vec2 dir = r > 0.0001 ? p / r : vec2(0.0, 1.0);
    float s = Strength;
    // 1. a twist round the gate, strongest close to it, breathing in and out
    float twist = s * (0.32 + 0.08 * sin(t * TAU / 3.0)) * exp(-r * 2.6);
    float ct = cos(twist);
    float st = sin(twist);
    vec2 q = vec2(p.x * ct - p.y * st, p.x * st + p.y * ct);
    // 2. the world is drawn in towards the gate
    q *= 1.0 - s * 0.10 * exp(-r * 2.2);
    // 3. heat shimmer
    q += vec2(sin(p.y * 38.0 + t * TAU / 2.0), cos(p.x * 33.0 + t * TAU / 3.0)) * 0.0032 * s;
    // 4. the shockwave: a ring of bent light rolling outwards
    float ring = 0.0;
    if (Shock > 0.0) {
        float d = r - Shock * 1.15;
        ring = exp(-d * d / 0.0045) * (1.0 - Shock * 0.8);
        q += dir * ring * 0.055;
    }
    vec2 uv = q / vec2(aspect, 1.0) + 0.5;
    // colours split along the radius
    vec2 split = dir / vec2(aspect, 1.0) * (0.0035 * s + 0.018 * ring);
    vec3 c;
    c.r = texture(InSampler, uv + split).r;
    c.g = texture(InSampler, uv).g;
    c.b = texture(InSampler, uv - split).b;
    // cyan light gathers: the whole picture cools, the gate's heart glows
    vec3 cyan = vec3(0.45, 1.0, 0.95);
    c = mix(c, c * vec3(0.78, 1.06, 1.12) + vec3(0.0, 0.05, 0.06), clamp(s * 0.7, 0.0, 1.0));
    c += cyan * (exp(-r * 4.0) * s * 0.32 + ring * 0.45);
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
