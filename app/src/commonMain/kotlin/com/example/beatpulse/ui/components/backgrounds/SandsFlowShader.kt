package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

const val SANDS_FLOW_SKSL = """
    uniform vec2 u_resolution;
    uniform float u_time;
    uniform float u_bass;
    uniform float u_treble;
    uniform vec4 u_dominant;
    uniform vec4 u_vibrant;
    uniform vec4 u_muted;

    float hash(vec2 p) {
        return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
    }

    float noise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        vec2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
                   mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
    }

    float fbm(vec2 p) {
        float value = 0.0;
        float amplitude = 0.5;
        mat2 rot = mat2(0.8, -0.6, 0.6, 0.8);
        for (int i = 0; i < 2; i++) { // Optimizacion: Reducido de 5 a 2
            value += amplitude * noise(p);
            p = rot * p * 2.0;
            amplitude *= 0.5;
        }
        return value;
    }

    vec4 main(vec2 fragCoord) {
        vec2 p = fragCoord.xy / u_resolution.xy;
        p.x *= u_resolution.x / u_resolution.y;

        float t = u_time * 0.1;
        
        // Campo de flujo direccional continuo (Flow Field optimizado)
        vec2 flow = vec2(
            sin(p.y * 2.0 + t) * 0.5,
            cos(p.x * 2.0 + t * 0.8) * 0.5
        );

        // Perturbación del mapa
        float n = fbm(p * 3.0 + flow * 2.5);
        
        // Simular las líneas topográficas / hilos de arena
        float lines = sin(n * 80.0);
        
        // Suavizar las líneas para que parezcan hilos finos y difuminados
        lines = smoothstep(0.0, 0.15, abs(lines));
        lines = 1.0 - lines; // Invertir para que las líneas brillen
        
        // Agregar zonas densas donde se agrupan las partículas
        float density = smoothstep(0.4, 0.7, sin(p.x * 4.0 - flow.x) * cos(p.y * 4.0 - flow.y) * 0.5 + 0.5);
        
        // Reacción al bajo para encender las líneas de arena
        float glow = (u_bass * 0.7) * lines * density;

        // Colores: Respetando el tono dominante, sin problemas de copyright
        vec3 bg = u_dominant.rgb * 0.15; // Fondo super oscuro
        vec3 sandColor = mix(u_muted.rgb, u_vibrant.rgb, density); 
        
        // Mezclamos el fondo oscuro con las líneas vibrantes
        vec3 finalColor = mix(bg, sandColor, lines * 0.4 + glow + density * 0.2);
        
        // Viñeteo
        float vignette = length(fragCoord.xy / u_resolution.xy - 0.5);
        finalColor *= smoothstep(0.8, 0.1, vignette);

        return vec4(finalColor, 1.0);
    }
"""

@Composable
expect fun SandsFlowShader(
    modifier: Modifier,
    time: Float,
    bass: Float,
    treble: Float,
    dominantColor: Color,
    vibrantColor: Color,
    mutedColor: Color
)
