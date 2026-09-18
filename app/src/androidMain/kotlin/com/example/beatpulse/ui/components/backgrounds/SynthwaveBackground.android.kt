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
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

private const val SYNTHWAVE_AGSL = """
    uniform float2 u_resolution;
    uniform float u_time;
    uniform float u_energy;
    uniform half4 u_dominant;
    uniform half4 u_vibrant;
    uniform half4 u_muted;

    const float speed = 2.0;
    const float audio_vibration_amplitude = 0.125;

    float amp(float2 p){
        return smoothstep(1.0, 8.0, abs(p.x));   
    }

    float pow1d5(float a){
        return a * sqrt(a);
    }

    float pow512(float a){
        a *= a; a *= a; a *= a; a *= a;
        a *= a; a *= a; a *= a; a *= a;
        return a * a;
    }

    float hash21(float2 co){
        return fract(sin(dot(co.xy, float2(1.9898, 7.233))) * 45758.5433);
    }

    float hash(float2 uv, float jTime){
        float a = amp(uv);
        float w = a > 0.0 ? (1.0 - 0.4 * pow512(0.51 + 0.49 * sin((0.02 * (uv.y + 0.5 * uv.x) - jTime) * 2.0))) : 0.0;
        float heightScale = 1.0 + u_energy * 0.8;
        float audioReactivity = u_energy * audio_vibration_amplitude;
        return (a > 0.0 ? a * pow1d5(hash21(uv)) * w * heightScale : 0.0) - audioReactivity;
    }

    float edgeMin(float dx, float2 da, float2 db, float2 uv){
        uv.x += 5.0;
        vec3 c = fract(floor(vec3(uv, uv.x + uv.y) + 0.5) * (vec3(0.0, 1.0, 2.0) + 0.61803398875));
        float a1 = (hash21(float2(c.y, 0.0)) * u_energy) > 0.6 ? 0.15 : 1.0;
        float a2 = (hash21(float2(c.x, 0.0)) * u_energy) > 0.6 ? 0.15 : 1.0;
        float a3 = (hash21(float2(c.z, 0.0)) * u_energy) > 0.6 ? 0.15 : 1.0;
        return min(min((1.0 - dx) * db.y * a3, da.x * a2), da.y * a1);
    }

    float2 trinoise(float2 uv, float jTime){
        const float sq = 1.22474487; 
        uv.x *= sq;
        uv.y -= 0.5 * uv.x;
        float2 d = fract(uv);
        uv -= d;

        bool c = dot(d, float2(1.0)) > 1.0;

        float2 dd = 1.0 - d;
        float2 da = c ? dd : d;
        float2 db = c ? d : dd;
        
        float nn = hash(uv + float(c), jTime);
        float n2 = hash(uv + float2(1.0, 0.0), jTime);
        float n3 = hash(uv + float2(0.0, 1.0), jTime);
        
        float nmid = mix(n2, n3, d.y);
        float ns = mix(nn, c ? n2 : n3, da.y);
        float dx = da.x / max(db.y, 0.0001);
        return float2(mix(ns, nmid, dx), edgeMin(dx, da, db, uv + d));
    }

    float2 map(vec3 p, float jTime){
        float2 n = trinoise(p.xz, jTime);
        return float2(p.y - 2.0 * n.x, n.y);
    }

    vec3 grad(vec3 p, float jTime){
        const float2 e = float2(0.005, 0.0);
        float a = map(p, jTime).x;
        return vec3(map(p + e.xyy, jTime).x - a,
                    map(p + e.yxy, jTime).x - a,
                    map(p + e.yyx, jTime).x - a) / e.x;
    }

    float2 intersect(vec3 ro, vec3 rd, float jTime){
        float d = 0.0, h = 0.0;
        for(int i = 0; i < 70; i++){
            vec3 p = ro + d * rd;
            float2 s = map(p, jTime);
            h = s.x;
            d += h * 0.35;
            if(abs(h) < 0.003 * d) return float2(d, s.y);
            if(d > 120.0 || p.y > 2.0) break;
        }
        return float2(-1.0);
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
        float c = 0.0;
        vec3 p = normalize(rd) * 300.0;
        for (float i = 0.0; i < 3.0; i++){
            vec3 q = fract(p) - 0.5;
            vec3 id = floor(p);
            float c2 = smoothstep(0.5, 0.0, length(q));
            c2 *= step(hash21(id.xz / max(abs(id.y), 0.001)), 0.06 - i * i * 0.005);
            c += c2;
            p = p * 0.6 + 0.5 * p * mat3(0.6, 0.0, 0.8, 0.0, 1.0, 0.0, -0.8, 0.0, 0.6);
        }
        c *= c;
        float g = dot(sin(rd * 10.512), cos(rd.yzx * 10.512));
        c *= smoothstep(-3.14, -0.9, g) * 0.5 + 0.5 * smoothstep(-0.3, 1.0, g);
        return c * c;
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
        float2 uv = (2.0 * fragCoord - u_resolution.xy) / u_resolution.y;
        uv.y = -uv.y; // Fix inverted Y
        
        float dt = fract(hash21(fragCoord) + u_time) * 0.25;
        float jTime = mod(u_time - dt * 0.016, 4000.0);
        vec3 ro = vec3(0.0, 1.0, (-20000.0 + jTime * speed));
        
        vec3 rd = normalize(vec3(uv, 1.3333333));
        
        float2 i = intersect(ro, rd, jTime);
        float d = i.x;
        
        vec3 ld = normalize(vec3(0.0, 0.125 + 0.05 * sin(0.1 * jTime), 1.0));

        vec3 fog = d > 0.0 ? exp2(-d * vec3(0.14, 0.1, 0.28)) : vec3(0.0);
        vec3 sky = gsky(rd, ld, d < 0.0);
        
        vec3 p = ro + d * rd;
        vec3 n = normalize(grad(p, jTime));
        
        float diff = dot(n, ld) + 0.1 * n.y;
        vec3 col = mix(vec3(u_dominant.rgb), vec3(u_muted.rgb), 0.6) * diff;
        
        vec3 rfd = reflect(rd, n); 
        vec3 rfcol = gsky(rfd, ld, true);
        
        col = mix(col, rfcol, 0.05 + 0.95 * pow(max(1.0 + dot(rd, n), 0.0), 5.0));
        
        col = mix(col, vec3(u_vibrant.rgb), smoothstep(0.05, 0.0, i.y));
        col = mix(sky, col, fog);
        col = sqrt(max(col, 0.0));
        
        if(d < 0.0) d = 1e6;
        d = min(d, 10.0);
        
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
            if (!currentIsPlayerScreen) kotlinx.coroutines.delay(24L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(dominantColorState.value)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val runtimeShader = remember {
                try {
                    RuntimeShader(SYNTHWAVE_AGSL)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val shaderBrush = remember(runtimeShader) {
                runtimeShader?.let { ShaderBrush(it) }
            }

            if (shaderBrush != null && runtimeShader != null) {
                val dom = dominantColorState.value
                val vib = vibrantColorState.value
                val mut = mutedColorState.value
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    runtimeShader.setFloatUniform("u_resolution", size.width, size.height)
                    runtimeShader.setFloatUniform("u_time", time)
                    runtimeShader.setFloatUniform("u_energy", dynamicEnergy)
                    runtimeShader.setFloatUniform("u_dominant", dom.red, dom.green, dom.blue, dom.alpha)
                    runtimeShader.setFloatUniform("u_vibrant", vib.red, vib.green, vib.blue, vib.alpha)
                    runtimeShader.setFloatUniform("u_muted", mut.red, mut.green, mut.blue, mut.alpha)
                    
                    drawRect(brush = shaderBrush, size = size)
                }
            }
        }
        
        content()
    }
}
