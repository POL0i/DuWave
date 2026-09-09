package com.example.beatpulse.ui.components.backgrounds

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

private const val SEA_THE_NIGHT_AGSL = """
uniform float2 u_resolution;
uniform float u_time;
uniform half4 u_dominant;
uniform half4 u_vibrant;
uniform float u_energy;

#define DISPLAY_GAMMA 1.9
#define GOLDEN_ANGLE 2.39996323
#define MAX_BLUR_SIZE 8.0
#define RAD_SCALE 2.0 
#define uFar 12.0
#define FOCUS_SCALE 35.0

float hash( float2 p ) {
    return fract(sin(dot(p,float2(127.1,311.7)))*43758.5453123);
}
float noise( in float2 p ) {
    float2 i = floor( p );
    float2 f = fract( p );
    float2 u = f*f*(3.0-2.0*f);
    return mix( mix( hash( i + float2(0.0,0.0) ), hash( i + float2(1.0,0.0) ), u.x),
                mix( hash( i + float2(0.0,1.0) ), hash( i + float2(1.0,1.0) ), u.x), u.y);
}
float fbm(float2 p) {
    float f = 0.0;
    f += 0.5000*noise( p ); p = p*2.02;
    f += 0.2500*noise( p ); p = p*2.03;
    f += 0.1250*noise( p );
    return f;
}

float4 renderSea(float2 uv) {
    float2 p = uv * 2.0 - 1.0;
    p.x *= u_resolution.x / u_resolution.y;
    
    float3 col = mix(u_dominant.rgb*0.2, u_vibrant.rgb*0.4, uv.y);
    float depth = 1.0;

    if (p.y < -0.1) {
        float d = -1.0 / (p.y + 0.1);
        float2 seaUv = p * d;
        seaUv.y -= u_time * 0.5;
        
        float waves = fbm(seaUv * 3.0 + u_time * 0.2);
        waves += fbm(seaUv * 6.0 - u_time * 0.4) * 0.5;
        
        float3 waterCol = mix(u_dominant.rgb * 0.5, u_vibrant.rgb, waves + u_energy);
        col = mix(waterCol, col, exp(-d * 0.1)); 
        depth = clamp(d / uFar, 0.0, 1.0);
    } else {
        float n = hash(p * 200.0 + u_time*0.01);
        if (n > 0.995) col += float3(min(1.0, n * 10.0));
    }
    return float4(col, depth);
}

float getBlurSize(float depth, float focusPoint, float focusScale) {
	float coc = clamp((1.0 / focusPoint - 1.0 / depth)*focusScale, -1.0, 1.0);
    return abs(coc) * MAX_BLUR_SIZE;
}

float3 depthOfField(float2 texCoord, float focusPoint, float focusScale) {
    float4 Input = renderSea(texCoord);
    float centerDepth = Input.a * uFar;
    float centerSize = getBlurSize(centerDepth, focusPoint, focusScale);
    float3 color = Input.rgb;
    float tot = 1.0;
    
    float2 texelSize = 1.0 / u_resolution.xy;
    float radius = RAD_SCALE;
    float ang = 0.0;
    for (int i = 0; i < 40; i++) {
        if (radius >= MAX_BLUR_SIZE) break;
        
        float2 tc = texCoord + float2(cos(ang), sin(ang)) * texelSize * radius;
        float4 sampleInput = renderSea(tc);
        float3 sampleColor = sampleInput.rgb;
        float sampleDepth = sampleInput.a * uFar;
        float sampleSize = getBlurSize(sampleDepth, focusPoint, focusScale);
        if (sampleDepth > centerDepth) {
        	sampleSize = clamp(sampleSize, 0.0, centerSize*2.0);
        }
        float m = smoothstep(radius-0.5, radius+0.5, sampleSize);
        color += mix(color/tot, sampleColor, m);
        tot += 1.0;
        radius += RAD_SCALE/radius;
        ang += GOLDEN_ANGLE;
    }
    return color /= tot;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord.xy / u_resolution.xy;
    float focusPoint = 58.0 - sin(u_time * 0.3) * 20.0;
    float3 color = depthOfField(uv, focusPoint, FOCUS_SCALE);
    color = float3(1.7, 1.8, 1.6) * color / (float3(1.0) + color);
	return half4(half3(pow(color, float3(1.0 / DISPLAY_GAMMA))), 1.0);
}
"""

@Composable
actual fun TerrariaWaterBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val bassAmplitudes by visualizerManager.bassAmplitudes.collectAsState()
    val bassAvg = remember(bassAmplitudes) { if (bassAmplitudes.isNotEmpty()) bassAmplitudes.average().toFloat().let { if (it.isNaN()) 0f else it } else 0f }
    
    val time = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    ).value

    val smoothedEnergy by animateFloatAsState(
        targetValue = bassAvg,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    val effectiveTime = if (isPlayerScreen) time * 0.6f else time * 0.25f
    val effectiveEnergy = if (isPlayerScreen) smoothedEnergy * 0.3f else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val runtimeShader = remember {
                try {
                    RuntimeShader(SEA_THE_NIGHT_AGSL)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val shaderBrush = remember(runtimeShader) {
                runtimeShader?.let { ShaderBrush(it) }
            }

            if (shaderBrush != null && runtimeShader != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    runtimeShader.setFloatUniform("u_resolution", size.width, size.height)
                    runtimeShader.setFloatUniform("u_time", effectiveTime)
                    runtimeShader.setFloatUniform("u_energy", effectiveEnergy)
                    runtimeShader.setFloatUniform("u_dominant", paletteColors.dominant.red, paletteColors.dominant.green, paletteColors.dominant.blue, paletteColors.dominant.alpha)
                    runtimeShader.setFloatUniform("u_vibrant", paletteColors.vibrant.red, paletteColors.vibrant.green, paletteColors.vibrant.blue, paletteColors.vibrant.alpha)
                    
                    drawRect(brush = shaderBrush)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(paletteColors.dominant))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(paletteColors.dominant))
        }
        
        content()
    }
}
