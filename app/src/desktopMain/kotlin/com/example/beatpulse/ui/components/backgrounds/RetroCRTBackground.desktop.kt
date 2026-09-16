package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.RuntimeEffect

private const val RETRO_CRT_SKSL = """
uniform float2 u_resolution;
uniform float u_time;
uniform float u_energy;
uniform half4 u_dominant;
uniform half4 u_vibrant;

const int iter = 90; 
const float sq = 0.70710678118; // sqrt(2.0)*0.5

float c(vec3 p) {
	vec3 q = abs(mod(p + vec3(cos(p.z*0.5), cos(p.x*0.5), cos(p.y*0.5)), 2.0) - 1.0);
	float a = q.x + q.y + q.z - min(min(q.x, q.y), q.z) - max(max(q.x, q.y), q.z);
	q = vec3(p.x+p.y, p.y+p.z, p.z+p.x)*sq;
	q = abs(mod(q, 2.0) - 1.0);
	float b = q.x + q.y + q.z - min(min(q.x, q.y), q.z) - max(max(q.x, q.y), q.z);
	return min(a,b);
}

vec3 n(vec3 p) {
    vec3 eps = vec3(0.01, 0.0, 0.0);
	float o = c(p);
	return normalize(o - vec3(c(p - eps), c(p - eps.zxy), c(p - eps.yzx)));
}

float2 CRTCurveUV(float2 uv) {
    uv = uv * 2.0 - 1.0;
    float2 offset = abs(uv.yx) / float2(6.0, 4.0);
    uv = uv + uv * offset * offset;
    uv = uv * 0.5 + 0.5;
    return uv;
}

vec3 DrawVignette(vec3 color, float2 uv) {    
    float vignette = uv.x * uv.y * (1.0 - uv.x) * (1.0 - uv.y);
    vignette = clamp(pow(16.0 * vignette, 0.3), 0.0, 1.0);
    return color * vignette;
}

vec3 DrawScanline(vec3 color, float2 uv, float t) {
    float scanline = clamp(0.95 + 0.05 * cos(3.14 * (uv.y + 0.008 * t) * 240.0 * 1.0), 0.0, 1.0);
    float grille   = 0.85 + 0.15 * clamp(1.5 * cos(3.14 * uv.x * 640.0 * 1.0), 0.0, 1.0);    
    return color * scanline * grille * 1.2;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord.xy / u_resolution.xy;
    float2 crtUV = CRTCurveUV(uv);
    
    if (crtUV.x < 0.0 || crtUV.x > 1.0 || crtUV.y < 0.0 || crtUV.y > 1.0) {
        return half4(0.0, 0.0, 0.0, 1.0);
    }
    
	float aspect = u_resolution.x / u_resolution.y;
	float2 p = crtUV * 2.0 - 1.0;
	p.x *= aspect;
	
    // Slow steady lateral drift to one direction (right)
	float2 m = float2(u_time * 0.02, 0.0);
	m.x *= aspect;
	
	// Gentle forward crawl (slower base) with slightly more momentary speed from energy
	vec3 o = vec3(0.0, 0.0, u_time * 0.05 + u_energy * 0.5);
	vec3 s = vec3(m, 0.0);
	vec3 b = vec3(0.0, 0.0, 0.0);
	// Just a little bit of zoom when energy rises (less aggressive)
	float zoom = 32.0 - u_energy * 2.0;
	vec3 d = vec3(p, 1.0) / zoom;
	vec3 t_val = vec3(0.5);
	
	for(int i = 0; i < iter; ++i) {
		float h = c(b + s + o);
		b += h * 10.0 * d;
		t_val += h;
	}
	t_val /= float(iter);
	vec3 a = n(b + s + o);
	float x = dot(a, t_val);
    
    // Fallback if vibrant is black
    vec3 diffuse = u_vibrant.rgb;
    if (length(diffuse) < 0.1) diffuse = vec3(0.0, 1.0, 0.3); // Matrix Green
    
	t_val = (t_val + pow(x, 4.0)) * (1.0 - t_val * 0.01) * diffuse;
	t_val *= b.z * 0.125; 

    vec3 color = t_val * 2.0;
    
    color = DrawVignette(color, crtUV);
    color = DrawScanline(color, uv, u_time);

    // Apply dominant color as background tint slowly
    color += u_dominant.rgb * 0.1;

	return half4(half3(color), 1.0);
}
"""

@Composable
actual fun RetroCRTBackground(
    dominantColor: Color,
    vibrantColor: Color,
    mutedColor: Color,
    dynamicEnergy: Float,
    dynamicOffsetY: Float,
    dynamicOffsetX: Float,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val time = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    ).value

    // Smooth the energy
    val smoothedEnergy by animateFloatAsState(
        targetValue = dynamicEnergy,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    val runtimeEffect = remember {
        try {
            RuntimeEffect.makeForShader(RETRO_CRT_SKSL)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Two speed modes: library (ultra-slow ambient) vs player (gentle energy reaction)
    val effectiveTime: Float
    val effectiveEnergy: Float
    if (isPlayerScreen) {
        effectiveTime = time * 0.6f
        effectiveEnergy = smoothedEnergy * 0.3f
    } else {
        effectiveTime = time * 0.25f
        effectiveEnergy = 0f
    }

    val shaderBrush = remember(runtimeEffect, effectiveTime, effectiveEnergy, dominantColor, vibrantColor) {
        runtimeEffect?.let { effect ->
            object : androidx.compose.ui.graphics.ShaderBrush() {
                override fun createShader(size: androidx.compose.ui.geometry.Size): androidx.compose.ui.graphics.Shader {
                    val builder = org.jetbrains.skia.RuntimeShaderBuilder(effect)
                    builder.uniform("u_resolution", size.width, size.height)
                    builder.uniform("u_time", effectiveTime)
                    builder.uniform("u_energy", effectiveEnergy)
                    builder.uniform("u_dominant", dominantColor.red, dominantColor.green, dominantColor.blue, dominantColor.alpha)
                    builder.uniform("u_vibrant", vibrantColor.red, vibrantColor.green, vibrantColor.blue, vibrantColor.alpha)
                    return builder.makeShader()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (shaderBrush != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(brush = shaderBrush)
            }
        } else {
            // Fallback
            Box(
                modifier = Modifier.fillMaxSize().background(dominantColor)
            )
        }
        
        content()
    }
}
