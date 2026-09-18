package com.example.beatpulse.ui.components.backgrounds

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

private const val WIND_WAKER_AGSL = """
    uniform float2 u_resolution;
    uniform float u_time;
    uniform float u_energy;
    uniform half4 u_water1;
    uniform half4 u_water2;
    uniform half4 u_foam;
    uniform half4 u_sky;

    const float M_2PI = 6.283185307;
    const float M_6PI = 18.84955592;

    float circ(float2 pos, float2 c, float s) {
        float r = (sin((u_time - (c.x * 10.0)) * 2.0) * 0.0015);
        c = abs(pos - c);
        c = min(c, 1.0 - c);
        return dot(c, c) < s - r ? -1.0 : 0.0;
    }

    float waterlayer(float2 uv) {
        uv = fract(uv); 
        float ret = 1.0;
        ret += circ(uv, float2(0.37378, 0.277169), 0.0268181);
        ret += circ(uv, float2(0.0317477, 0.540372), 0.0193742);
        ret += circ(uv, float2(0.430044, 0.882218), 0.0232337);
        ret += circ(uv, float2(0.641033, 0.695106), 0.0117864);
        ret += circ(uv, float2(0.0146398, 0.0791346), 0.0299458);
        ret += circ(uv, float2(0.43871, 0.394445), 0.0289087);
        ret += circ(uv, float2(0.909446, 0.878141), 0.028466);
        ret += circ(uv, float2(0.310149, 0.686637), 0.0128496);
        ret += circ(uv, float2(0.928617, 0.195986), 0.0152041);
        ret += circ(uv, float2(0.0438506, 0.868153), 0.0268601);
        ret += circ(uv, float2(0.308619, 0.194937), 0.00806102);
        ret += circ(uv, float2(0.349922, 0.449714), 0.00928667);
        ret += circ(uv, float2(0.0449556, 0.953415), 0.023126);
        ret += circ(uv, float2(0.117761, 0.503309), 0.0151272);
        ret += circ(uv, float2(0.563517, 0.244991), 0.0292322);
        ret += circ(uv, float2(0.566936, 0.954457), 0.00981141);
        ret += circ(uv, float2(0.0489944, 0.200931), 0.0178746);
        ret += circ(uv, float2(0.569297, 0.624893), 0.0132408);
        ret += circ(uv, float2(0.298347, 0.710972), 0.0114426);
        ret += circ(uv, float2(0.878141, 0.771279), 0.00322719);
        ret += circ(uv, float2(0.150995, 0.376221), 0.00216157);
        ret += circ(uv, float2(0.119673, 0.541984), 0.0124621);
        ret += circ(uv, float2(0.629598, 0.295629), 0.0198736);
        ret += circ(uv, float2(0.334357, 0.266278), 0.0187145);
        ret += circ(uv, float2(0.918044, 0.968163), 0.0182928);
        return max(ret, 0.0);
    }

    vec3 water(float2 uv, vec3 cdir) {
        uv *= float2(0.25, 0.25);
        
        float2 a = 0.025 * cdir.xz / cdir.y;
        float h = sin(uv.x + u_time);
        uv += a * h;
        h = sin(0.841471 * uv.x - 0.540302 * uv.y + u_time);
        uv += a * h;
        
        float d1 = mod(uv.x + uv.y, M_2PI);
        float d2 = mod((uv.x + uv.y + 0.25) * 1.3, M_6PI);
        d1 = u_time * 0.1 + d1;
        d2 = u_time * 0.6 + d2;
        float2 dist = float2(
            sin(d1) * 0.05 + sin(d2) * 0.05,
            cos(d1) * 0.05 + cos(d2) * 0.05
        );
        
        dist += float2(u_energy * 0.1, u_energy * 0.1);

        vec3 ret = mix(vec3(u_water1.rgb), vec3(u_water2.rgb), waterlayer(uv + dist.xy));
        ret = mix(ret, vec3(u_foam.rgb), waterlayer(float2(0.1 * u_time, 1.0) - uv - dist.yx));
        return ret;
    }

    vec3 pixtoray(float2 uv) {
        vec3 pixpos;
        pixpos.xy = uv - 0.5;
        pixpos.y *= u_resolution.y / u_resolution.x; 
        pixpos.z = -0.6; 
        return normalize(pixpos);
    }

    vec3 quatmul(vec4 q, vec3 v) {
        vec3 qvec = q.xyz;
        vec3 uv = cross(qvec, v);
        vec3 uuv = cross(qvec, uv);
        uv *= (2.0 * q.w);
        uuv *= 2.0;
        return v + uv + uuv;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord.xy / u_resolution.xy;
        vec3 cpos = vec3(0.0, 4.0, 10.0);
        
        cpos.y += u_energy * 0.8; 
        
        vec3 cdir = pixtoray(uv);
        cdir = quatmul(vec4(-0.19867, 0.0, 0.0, 0.980067), cdir); 
        
        float baseTime = u_time * -0.05;
        float cost = cos(baseTime);
        float sint = sin(baseTime);
        cdir.xz = cost * cdir.xz + sint * float2(-cdir.z, cdir.x);
        cpos.xz = cost * cpos.xz + sint * float2(-cpos.z, cpos.x);

        vec3 ocean = vec3(0.0, 1.0, 0.0);
        float dist = -dot(cpos, ocean) / dot(cdir, ocean);
        vec3 pos = cpos + dist * cdir;

        vec3 pix;
        vec3 skyColor = vec3(u_sky.rgb);
        vec3 fogCol = mix(skyColor, vec3(1.0, 1.0, 1.0), 0.3); 

        if(dist > 0.0 && dist < 100.0) {
            vec3 wat = water(pos.xz, cdir);
            pix = mix(wat, fogCol, min(dist * 0.01, 1.0));
        } else {
            pix = mix(fogCol, skyColor, min(cdir.y * 4.0, 1.0));
        }
        
        return half4(half3(clamp(pix, 0.0, 1.0)), 1.0);
    }
"""

@Composable
actual fun WindWakerOceanBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        WindWakerOceanBackgroundTiramisu(paletteColors, visualizerManager, isPlayerScreen, modifier, content)
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(paletteColors.dominant ?: Color.DarkGray)
        ) {
            content()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun WindWakerOceanBackgroundTiramisu(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shader = remember { RuntimeShader(WIND_WAKER_AGSL) }
    var time by remember { mutableFloatStateOf(0f) }
    var dynamicEnergy by remember { mutableFloatStateOf(0f) }
    val brush = remember(shader) { ShaderBrush(shader) }
    
    val amplitudesState = visualizerManager.combinedAmplitudes.collectAsState()
    var smoothEnergy by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlayerScreen) {
        var lastTime = 0L
        while (true) {
            androidx.compose.runtime.withFrameMillis { frameTime ->
                if (lastTime == 0L) lastTime = frameTime
                val dt = ((frameTime - lastTime) / 1000f).coerceAtMost(0.1f)
                lastTime = frameTime
                
                val currentAmps = amplitudesState.value
                var sumAmps = 0f
                val limit = if (currentAmps.size < 6) currentAmps.size else 6
                for (i in 0 until limit) {
                    sumAmps += currentAmps[i]
                }
                val rawEnergy = if (limit > 0) sumAmps / limit else 0f
                
                smoothEnergy += (rawEnergy - smoothEnergy) * (1f - kotlin.math.exp(-15f * dt))
                dynamicEnergy = smoothEnergy * (if (isPlayerScreen) 1.5f else 0.4f)
                
                val speedBoost = smoothEnergy * 1.5f
                time += dt * (if (isPlayerScreen) 1.0f + speedBoost else 0.4f + speedBoost * 0.2f)
            }
            if (!isPlayerScreen) kotlinx.coroutines.delay(24L)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val dom = paletteColors.dominant ?: Color.DarkGray
        val vib = paletteColors.vibrant ?: dom
        val light = paletteColors.lightVibrant ?: Color.White

        Canvas(modifier = Modifier.fillMaxSize()) {
            shader.setFloatUniform("u_resolution", size.width, size.height)
            shader.setFloatUniform("u_time", time)
            shader.setFloatUniform("u_energy", dynamicEnergy)
            shader.setColorUniform("u_water1", android.graphics.Color.valueOf(dom.red, dom.green, dom.blue, 1f).toArgb())
            shader.setColorUniform("u_water2", android.graphics.Color.valueOf(vib.red, vib.green, vib.blue, 1f).toArgb())
            shader.setColorUniform("u_foam", android.graphics.Color.valueOf(light.red, light.green, light.blue, 1f).toArgb())
            shader.setColorUniform("u_sky", android.graphics.Color.valueOf(dom.red, dom.green, dom.blue, 1f).toArgb())
            
            drawRect(brush = brush, size = size)
        }
        
        content()
    }
}
