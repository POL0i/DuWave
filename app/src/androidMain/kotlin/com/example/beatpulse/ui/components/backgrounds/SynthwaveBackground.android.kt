package com.example.beatpulse.ui.components.backgrounds

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.animateColorAsState
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

private const val SYNTHWAVE_AGSL = """
    uniform float2 u_resolution;
    uniform float u_time;
    uniform float u_energy;
    uniform half4 u_dominant;
    uniform half4 u_vibrant;
    uniform half4 u_muted;

    const float speed = 1.0;
    const float audio_vibration_amplitude = 0.125;

    float amp(float2 p){
        float ax = abs(p.x);
        return ax > 8.0 ? 1.0 : (ax < 1.0 ? 0.0 : (ax - 1.0) / 7.0);
    }

    float pow1d5(float a){
        return a * sqrt(a);
    }

    float pow16(float a){
        a *= a; a *= a; a *= a; a *= a;
        return a;
    }

    float hash21(float2 p){
        vec3 p3  = fract(vec3(p.xyx) * 0.1031);
        p3 += dot(p3, p3.yzx + 33.33);
        return fract((p3.x + p3.y) * p3.z);
    }

    float hash(float2 uv){
        float a = amp(uv);
        if (a <= 0.0) return -u_energy * audio_vibration_amplitude;
        
        float h = hash21(uv);
        return a * (h * h) * (1.0 + u_energy * 0.8) - (u_energy * audio_vibration_amplitude);
    }

    float edgeMin(float dx, float2 da, float2 db){
        return min(min((1.0 - dx) * db.y, da.x), da.y);
    }

    float trinoiseHeight(float2 uv){
        const float sq = 1.22474487; 
        uv.x *= sq;
        uv.y -= 0.5 * uv.x;
        float2 d = fract(uv);
        uv -= d;
        bool c = dot(d, float2(1.0)) > 1.0;
        float2 dd = 1.0 - d;
        float2 da = c ? dd : d;
        float2 db = c ? d : dd;
        float nn = hash(uv + float(c));
        float n2 = hash(uv + float2(1.0, 0.0));
        float n3 = hash(uv + float2(0.0, 1.0));
        float nmid = mix(n2, n3, d.y);
        float ns = mix(nn, c ? n2 : n3, da.y);
        float dx = da.x / max(db.y, 0.0001);
        return mix(ns, nmid, dx);
    }
    
    float trinoiseEdge(float2 uv){
        const float sq = 1.22474487; 
        uv.x *= sq;
        uv.y -= 0.5 * uv.x;
        float2 d = fract(uv);
        uv -= d;
        bool c = dot(d, float2(1.0)) > 1.0;
        float2 dd = 1.0 - d;
        float2 da = c ? dd : d;
        float2 db = c ? d : dd;
        float dx = da.x / max(db.y, 0.0001);
        return edgeMin(dx, da, db);
    }

    float map(vec3 p){
        float a = amp(p.xz);
        if (a <= 0.0) return p.y - 2.0 * (-u_energy * audio_vibration_amplitude);
        return p.y - 2.0 * trinoiseHeight(p.xz);
    }

    vec3 grad(vec3 p){
        const float2 e = float2(0.005, 0.0);
        float a = map(p);
        return vec3(map(p + e.xyy) - a,
                    map(p + e.yxy) - a,
                    map(p + e.yyx) - a) / e.x;
    }

    float intersect(vec3 ro, vec3 rd){
        float d = 0.0, h = 0.0;
        for(int i = 0; i < MAX_STEPS; i++){
            vec3 p = ro + d * rd;
            h = map(p);
            d += h; // step 1.0
            if(abs(h) < 0.005 * d) return d;
            if(d > MAX_DIST || p.y > 2.0) return -1.0;
        }
        return d; 
    }

    void addsun(vec3 rd, vec3 ld, inout vec3 col){
        float sun = smoothstep(0.21, 0.2, distance(rd, ld));
        if(sun > 0.0){
            float yd = (rd.y - ld.y);
            float a = sin(3.1 * exp(-(yd) * 14.0)); 
            sun *= smoothstep(-0.8, 0.0, a);
            vec3 sunColor = mix(vec3(u_vibrant.rgb), vec3(u_dominant.rgb) + vec3(0.2), 0.3);
            col = mix(col, sunColor * 0.9, sun);
        }
    }

    float starnoise(vec3 rd){
        vec3 p = normalize(rd) * 300.0;
        vec3 id = floor(p);
        float c = smoothstep(0.5, 0.0, length(fract(p) - 0.5));
        return c * step(hash21(id.xz), 0.01);
    }

    vec3 gsky(vec3 rd, vec3 ld, bool mask){
        float haze = exp2(-5.0 * (abs(rd.y) - 0.2 * dot(rd, ld)));
        float st = mask ? (starnoise(rd)) * (1.0 - min(haze, 1.0)) : 0.0;
        
        vec3 back = mix(vec3(u_dominant.rgb), vec3(u_vibrant.rgb), 0.3) * 0.5 * (1.0 - 0.5 * u_energy * exp2(-0.1 * abs(length(rd.xz)/max(abs(rd.y), 0.001))) * max(sign(rd.y), 0.0));
        
        vec3 col = clamp(mix(back, vec3(u_muted.rgb), haze) + st, 0.0, 1.0);
        if(mask) addsun(rd, ld, col);
        return col;  
    }

    half4 main(float2 fragCoord) {
        float2 uv = (2.0 * fragCoord - u_resolution.xy) / min(u_resolution.x, u_resolution.y);
        uv.y = -uv.y; // Fix inverted Y
        
        float jTime = mod(u_time, 4000.0);
        vec3 ro = vec3(0.0, 1.0, (-20000.0 + jTime * speed));
        
        vec3 rd = normalize(vec3(uv, 1.3333333));
        
        RAYMARCH_CALL
        
        vec3 ld = normalize(vec3(0.0, 0.125 + 0.05 * sin(0.1 * jTime), 1.0));
        vec3 col = vec3(0.0);
        
        if (d < 0.0) {
            col = gsky(rd, ld, true);
        } else {
            vec3 p = ro + d * rd;
            
            // Simplified Retro Shading: no normals, no reflections, just flat dark terrain and glowing grid
            vec3 terrainCol = mix(vec3(u_dominant.rgb), vec3(u_muted.rgb), 0.6) * 0.15;
            vec3 fog = exp2(-d * vec3(0.14, 0.1, 0.28));
            
            // Only calculate glowing edges if it's close enough (not completely hidden by fog)
            if (fog.y > 0.05) {
                float edge = trinoiseEdge(p.xz);
                float glow = smoothstep(0.08, 0.0, edge) + smoothstep(0.02, 0.0, edge) * 0.8;
                terrainCol = mix(terrainCol, vec3(u_vibrant.rgb), min(glow, 1.0));
            }
            
            vec3 fogColor = mix(vec3(u_dominant.rgb) * 0.1, vec3(u_muted.rgb), exp2(-5.0 * abs(rd.y)));
            col = mix(fogColor, terrainCol, fog);
        }
        
        col = sqrt(max(col, 0.0));
        vec3 finalColor = clamp(col, 0.0, 1.0);
        return half4(half3(finalColor), 1.0); 
    }
"""

@Composable
actual fun SynthwaveBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val dominantColorState = animateColorAsState(targetValue = paletteColors.dominant, tween(1500), label = "sw_dom")
    val vibrantColorState = animateColorAsState(targetValue = paletteColors.vibrant, tween(1500), label = "sw_vib")
    val mutedColorState = animateColorAsState(targetValue = paletteColors.muted, tween(1500), label = "sw_mut")

    val amplitudesState = visualizerManager.combinedAmplitudes.collectAsState()
    val currentIsPlayerScreen by rememberUpdatedState(isPlayerScreen)
    var dynamicEnergy by remember { mutableFloatStateOf(0f) }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var smoothEnergy = 0f
        var lastTime = 0L
        while (true) {
            withFrameMillis { frameTime ->
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
                dynamicEnergy = smoothEnergy * (if (currentIsPlayerScreen) 1.5f else 0.4f)
                
                val speedBoost = smoothEnergy * 2.5f
                time += dt * (if (currentIsPlayerScreen) 1.5f + speedBoost else 0.5f + speedBoost * 0.3f)
            }
            if (!currentIsPlayerScreen) kotlinx.coroutines.delay(120L) else kotlinx.coroutines.delay(24L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(dominantColorState.value)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            var runtimeShader by remember { mutableStateOf<RuntimeShader?>(null) }
            LaunchedEffect(isPlayerScreen) {
                if (!isPlayerScreen) kotlinx.coroutines.delay(100L)
                val shader = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    try {
                        var shaderCode = SYNTHWAVE_AGSL
                            .replace("MAX_STEPS", if (isPlayerScreen) "15" else "0")
                            .replace("MAX_DIST", if (isPlayerScreen) "45.0" else "0.0")
                            .replace("RAYMARCH_CALL", if (isPlayerScreen) "float d = intersect(ro, rd);" else "float d = -1.0;")
                        
                        if (!isPlayerScreen) {
                            shaderCode = shaderCode.replace("float st = mask ? (starnoise(rd)) * (1.0 - min(haze, 1.0)) : 0.0;", "float st = 0.0;")
                        }
                        RuntimeShader(shaderCode)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                runtimeShader = shader
            }

            val brush = remember(runtimeShader) {
                runtimeShader?.let { ShaderBrush(it) }
            }

            if (brush != null && runtimeShader != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    runtimeShader?.setFloatUniform("u_resolution", size.width, size.height)
                    runtimeShader?.setFloatUniform("u_time", time)
                    runtimeShader?.setFloatUniform("u_energy", dynamicEnergy)
                    runtimeShader?.setFloatUniform("u_dominant", dominantColorState.value.red, dominantColorState.value.green, dominantColorState.value.blue, dominantColorState.value.alpha)
                    runtimeShader?.setFloatUniform("u_vibrant", vibrantColorState.value.red, vibrantColorState.value.green, vibrantColorState.value.blue, vibrantColorState.value.alpha)
                    runtimeShader?.setFloatUniform("u_muted", mutedColorState.value.red, mutedColorState.value.green, mutedColorState.value.blue, mutedColorState.value.alpha)
                    
                    drawRect(brush = brush, size = size)
                }
            }
        }
        
        content()
    }
}
