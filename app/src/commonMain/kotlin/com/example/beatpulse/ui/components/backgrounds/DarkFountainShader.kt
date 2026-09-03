package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

const val DARK_FOUNTAIN_SKSL = """
    uniform vec2 u_resolution;
    uniform float u_time;
    uniform float u_bass;
    uniform float u_treble;
    uniform vec4 u_dominant;
    uniform vec4 u_vibrant;

    float hash(vec2 p) {
        return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
    }

    float noise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        vec2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(hash(i + vec2(0.0, 0.0)), 
                       hash(i + vec2(1.0, 0.0)), u.x),
                   mix(hash(i + vec2(0.0, 1.0)), 
                       hash(i + vec2(1.0, 1.0)), u.x), u.y);
    }

    float fbm(vec2 p) {
        float value = 0.0;
        float amplitude = 0.5;
        for (int i = 0; i < 2; i++) { // Optimized: reduced octaves from 4 to 2
            value += amplitude * noise(p);
            p *= 2.0;
            amplitude *= 0.5;
        }
        return value;
    }

    vec4 main(vec2 fragCoord) {
        // Pixelation effect (optimized: larger blocks mean less detail to render perceptually, though math is still per-pixel)
        float pixelsY = 70.0;
        float pixelSize = max(1.0, u_resolution.y / pixelsY);
        vec2 pCoord = floor(fragCoord / pixelSize) * pixelSize;
        
        vec2 uv = pCoord / u_resolution.xy;
        float aspect = u_resolution.x / u_resolution.y;
        vec2 p = vec2((uv.x - 0.5) * aspect, uv.y);
        
        float time = u_time * 0.8;
        
        // Base width dynamic based on aspect ratio to leave space on phones
        // We want it to occupy max ~60-70% of the screen width
        float baseWidth = 0.3 * aspect; 
        
        float waveY = floor(p.y * 12.0) / 12.0; 
        
        // Irregular wavy distortion using FBM
        float irregularWave = fbm(vec2(waveY * 2.0, time * 0.4)) - 0.5; 
        float irregularWave2 = sin(waveY * 8.0 - time * 1.5) * 0.02 * aspect;
        
        float distortion = irregularWave * 0.2 * aspect + irregularWave2;
                           
        // Fix the base ONLY at the very bottom (last 5% of screen)
        // p.y = 1.0 is bottom. mobility is 0 at bottom, 1 at 0.95
        float mobility = smoothstep(1.0, 0.95, p.y);
        
        distortion *= mobility;
                           
        // Irregular thickness variation
        float thicknessModulation = fbm(vec2(p.y * 4.0, time * 0.5)) * 0.06 * aspect
                                  + sin(p.y * 6.0 + time * 2.0) * (0.02 + u_bass * 0.08) * aspect;
        thicknessModulation *= mobility; 
                           
        // Combine width factors
        float edgeScale = 1.0 + (p.y - 0.5) * (p.y - 0.5) * 0.3; 
        float edge = (baseWidth + thicknessModulation) * edgeScale;
                           
        float dist = abs(p.x - distortion);
        
        vec4 color = vec4(0.0);
        
        // Scale thickness and gap based on aspect so it fits phones
        float thickness = 0.015 * aspect; 
        float gap = 0.025 * aspect;       
        
        float d1 = edge;
        float d2 = edge + gap;
        float d3 = edge + gap * 2.0;
        
        if (dist < d1 - thickness) {
            // Noise advances from bottom to top-left
            // p.x + time (moves left), p.y + time (moves up)
            float n1 = noise(vec2(p.x * 20.0 + time * 4.0, p.y * 10.0 + time * 8.0));
            float n2 = noise(vec2(p.x * 40.0 + time * 6.0, p.y * 20.0 + time * 12.0));
            float dirt = (n1 + n2) * 0.5;
            
            float sharpDirt = step(0.5, dirt);
            
            // Reused simpler math for holes instead of full FBM
            float holes = noise(vec2(p.x * 4.0 + time * 1.0, p.y * 3.0 + time * 2.0));
            float sharpHoles = step(0.65, holes);
            
            vec4 dirtColor = mix(u_dominant * 0.4, u_vibrant, sharpDirt);
            if (sharpHoles > 0.0) {
                dirtColor = vec4(0.0, 0.0, 0.0, 1.0); // Pitch black holes
            }
            
            color = dirtColor;
        } 
        else if (dist >= d1 - thickness && dist < d1) {
            color = u_vibrant; // Line 1
        } 
        else if (dist >= d2 - thickness && dist < d2) {
            color = mix(u_dominant, u_vibrant, 0.6); // Line 2
        } 
        else if (dist >= d3 - thickness && dist < d3) {
            color = u_dominant; // Line 3
        } 
        else {
            color = vec4(0.0); // Background
        }
        
        return color;
    }
"""

@Composable
expect fun DarkFountainShader(
    modifier: Modifier,
    time: Float,
    bass: Float,
    treble: Float,
    dominantColor: Color,
    vibrantColor: Color
)
