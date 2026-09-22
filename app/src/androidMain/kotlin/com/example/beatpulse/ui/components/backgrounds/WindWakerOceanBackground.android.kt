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
import androidx.compose.ui.graphics.toArgb
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

private const val WIND_WAKER_AGSL = """
    uniform float2 u_resolution;
    uniform float u_time;
    uniform float u_energy;
    uniform half4 u_color1;
    uniform half4 u_color2;
    uniform half4 u_color3;
    uniform half4 u_color4;
    uniform half4 u_color5;
    uniform half4 u_color6;
    uniform half4 u_color7;
    uniform half4 u_color8;
    uniform half4 u_color9;

    const float M_2PI = 6.283185307;
    const float M_6PI = 18.84955592;

    float circ(float2 pos, float2 c, float s) {
        c = abs(pos - c);
        c = min(c, 1.0 - c);
        return dot(c, c) < s ? -1.0 : 0.0;
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
        // INSERT_EXTRA_CIRCS
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

        float w1 = waterlayer(uv + dist.xy);
        
        vec3 baseWater = mix(vec3(u_color1.rgb), vec3(u_color3.rgb), w1);
        vec3 ret = baseWater;
        
        if (IS_PLAYER == 1) {
            vec3 baseWater2 = mix(vec3(u_color7.rgb), vec3(u_color8.rgb), w1);
            baseWater = mix(baseWater, baseWater2, sin(u_time * 0.3) * 0.5 + 0.5);
            ret = baseWater;
            
            float w2 = waterlayer(uv * 1.5 - dist.yx + float2(u_time * 0.02, u_time * 0.02));
            float foam = waterlayer(float2(0.1 * u_time, 1.0) - uv - dist.yx);
            
            vec3 midColor = mix(vec3(u_color2.rgb), vec3(u_color4.rgb), sin((uv.x + uv.y) * 4.0 + u_time) * 0.5 + 0.5);
            ret = mix(baseWater, midColor, w2 * 0.85);
            
            vec3 foamColor = mix(vec3(u_color5.rgb), vec3(u_color9.rgb), cos(uv.x * 8.0 - u_time * 1.5) * 0.5 + 0.5);
            ret = mix(ret, foamColor, foam);
        } else {
            // Highly optimized thumbnail: Only two layers (base water and foam), no complex gradients
            float foam = waterlayer(float2(0.1 * u_time, 1.0) - uv - dist.yx);
            ret = mix(baseWater, vec3(u_color9.rgb), foam);
        }
        
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
        uv.y = 1.0 - uv.y;
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
        vec3 skyColor = vec3(u_color6.rgb);
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
        var runtimeShader by remember { mutableStateOf<RuntimeShader?>(null) }
        LaunchedEffect(isPlayerScreen) {
            if (!isPlayerScreen) kotlinx.coroutines.delay(100L)
            val shader = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    val extraCircs = if (isPlayerScreen) """
                        ret += circ(uv, float2(0.928617, 0.195986), 0.0152041);
                        ret += circ(uv, float2(0.0438506, 0.868153), 0.0268601);
                        ret += circ(uv, float2(0.308619, 0.194937), 0.00806102);
                        ret += circ(uv, float2(0.349922, 0.449714), 0.00928667);
                        ret += circ(uv, float2(0.0449556, 0.953415), 0.023126);
                        ret += circ(uv, float2(0.117761, 0.503309), 0.0151272);
                        ret += circ(uv, float2(0.563517, 0.244991), 0.0292322);
                        ret += circ(uv, float2(0.566936, 0.954457), 0.00981141);
                    """ else ""
                    var shaderCode = WIND_WAKER_AGSL
                        .replace("// INSERT_EXTRA_CIRCS", extraCircs)
                        .replace("IS_PLAYER", if (isPlayerScreen) "1" else "0")
                    RuntimeShader(shaderCode)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            runtimeShader = shader
        }
        var time by remember { mutableFloatStateOf(0f) }
        var dynamicEnergy by remember { mutableFloatStateOf(0f) }
        val brush = remember(runtimeShader) { runtimeShader?.let { ShaderBrush(it) } }
        
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
                if (!isPlayerScreen) kotlinx.coroutines.delay(120L) else kotlinx.coroutines.delay(24L)
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            val dom = paletteColors.dominant ?: Color.DarkGray
            val vib = paletteColors.vibrant ?: dom
            val mut = paletteColors.muted ?: dom
            val dvib = paletteColors.darkVibrant ?: vib
            val lvib = paletteColors.lightVibrant ?: Color.White
            val dmut = paletteColors.darkMuted ?: Color.Black
            val ext1 = paletteColors.extra1 ?: dom
            val ext2 = paletteColors.extra2 ?: vib
            val ext3 = paletteColors.extra3 ?: mut

            Canvas(modifier = Modifier.fillMaxSize()) {
                runtimeShader?.let { shader ->
                    shader.setFloatUniform("u_resolution", size.width, size.height)
                    shader.setFloatUniform("u_time", time)
                    shader.setFloatUniform("u_energy", dynamicEnergy)
                    shader.setFloatUniform("u_color1", dom.red, dom.green, dom.blue, dom.alpha)
                    shader.setFloatUniform("u_color2", vib.red, vib.green, vib.blue, vib.alpha)
                    shader.setFloatUniform("u_color3", mut.red, mut.green, mut.blue, mut.alpha)
                    shader.setFloatUniform("u_color4", dvib.red, dvib.green, dvib.blue, dvib.alpha)
                    shader.setFloatUniform("u_color5", lvib.red, lvib.green, lvib.blue, lvib.alpha)
                    shader.setFloatUniform("u_color6", dmut.red, dmut.green, dmut.blue, dmut.alpha)
                    shader.setFloatUniform("u_color7", ext1.red, ext1.green, ext1.blue, ext1.alpha)
                    shader.setFloatUniform("u_color8", ext2.red, ext2.green, ext2.blue, ext2.alpha)
                    shader.setFloatUniform("u_color9", ext3.red, ext3.green, ext3.blue, ext3.alpha)
                }
                
                brush?.let { drawRect(brush = it, size = size) }
            }
            
            content()
        }
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
