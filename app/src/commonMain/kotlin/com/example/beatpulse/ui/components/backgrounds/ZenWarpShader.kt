package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

const val ZEN_WARP_SKSL = """
    uniform vec2 u_resolution;
    uniform float u_time;
    uniform float u_bass;
    uniform float u_treble;
    uniform vec4 u_dominant;
    uniform vec4 u_vibrant;
    uniform vec4 u_muted;

    // --- Funciones matemáticas base (Dominio público / Algoritmos estándar) ---
    float hash(vec2 p) {
        return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
    }

    float noise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        // Quintic interpolation for smoother gradients (eliminates visible grid artifacts)
        vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
        return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
                   mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
    }

    // Fractional Brownian Motion - 2 octavas (optimizado para móvil)
    float fbm(vec2 p) {
        float value = 0.0;
        float amplitude = 0.5;
        mat2 rot = mat2(0.8, -0.6, 0.6, 0.8);
        for (int i = 0; i < 2; i++) {
            value += amplitude * noise(p);
            p = rot * p * 2.0;
            amplitude *= 0.5;
        }
        return value;
    }

    vec4 main(vec2 fragCoord) {
        vec2 p = fragCoord.xy / u_resolution.xy;
        p.x *= u_resolution.x / u_resolution.y;

        float t = u_time * 0.12;
        float audioReact = u_bass * 0.4;

        // Capa 1: deformación suave y lenta (optimizado con trigonometría en lugar de ruido)
        vec2 warp1 = vec2(
            sin(p.y * 3.0 + t) * 0.5 + cos(p.x * 2.0 + t * 0.7) * 0.5,
            cos(p.x * 3.0 - t * 0.8) * 0.5 + sin(p.y * 2.5 + t * 0.6) * 0.5
        );
        
        // Capa 2: efecto orgánico con 1 fbm + offset senoidal (Optimizado a noise simple)
        float w2base = noise(p * 3.0 + warp1 * 1.5 + vec2(t * 0.8, t * 0.5));
        vec2 warp2 = vec2(
            w2base,
            w2base * 0.8 + sin(p.x * 4.0 + warp1.y * 3.0 + t) * 0.2
        );

        // Terreno final suavizado
        float terrain = fbm(p * 3.5 + warp2 * 2.0);

        // --- COLORIZACIÓN ---
        vec3 finalColor = u_dominant.rgb;
        
        // Transiciones suaves hacia Muted y Vibrant
        float factorMuted = smoothstep(0.25, 0.55, terrain);
        finalColor = mix(finalColor, u_muted.rgb, factorMuted);
        
        float factorVibrant = smoothstep(0.5, 0.85, terrain);
        finalColor = mix(finalColor, u_vibrant.rgb, factorVibrant);
        
        // Profundidad suave basada en el terreno
        float depth = smoothstep(0.0, 1.0, terrain);
        finalColor *= (0.55 + 0.45 * depth);
        
        // Brillo sutil en crestas, reactivo al audio
        float highlight = smoothstep(0.75, 1.0, terrain);
        finalColor += u_vibrant.rgb * highlight * (0.15 + audioReact * 0.4);

        // Viñeteo suave
        float vignette = length(fragCoord.xy / u_resolution.xy - 0.5);
        finalColor *= smoothstep(0.8, 0.25, vignette);

        return vec4(finalColor, 1.0);
    }
"""

@Composable
expect fun ZenWarpShader(
    modifier: Modifier,
    time: State<Float>,
    bass: State<Float>,
    treble: State<Float>,
    dominantColor: Color,
    vibrantColor: Color,
    mutedColor: Color
)
