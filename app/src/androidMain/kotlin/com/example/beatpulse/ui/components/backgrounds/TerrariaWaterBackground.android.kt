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
uniform vec2 u_resolution;
uniform float u_time;
uniform vec4 u_dominant;
uniform vec4 u_vibrant;
uniform float u_energy;
uniform float u_is_player;

float myMod(float x, float y) {
    return x - y * floor(x / y);
}

float Func(float pX) {
	return 0.6*(0.5*sin(0.1*pX) + 0.5*sin(0.553*pX) + 0.7*sin(1.2*pX));
}

float FuncR(float pX) {
	return 0.5 + 0.25*(1.0 + sin(myMod(40.0*pX, 6.28318530718)));
}

float Layer(vec2 pQ, float pT) {
	vec2 Qt = 3.5*pQ;
	pT *= 0.5;
	Qt.x += pT;

	float Xi = floor(Qt.x);
	float Xf = Qt.x - Xi - 0.5;

	vec2 C;
	float Yi;
	float D = 1.0 - step(Qt.y, Func(Qt.x));

	Yi = Func(Xi + 0.5);
	C = vec2(Xf, Qt.y - Yi );
	D = min(D, length(C) - FuncR(Xi+ pT/80.0));

	Yi = Func(Xi+1.0 + 0.5);
	C = vec2(Xf-1.0, Qt.y - Yi );
	D = min(D, length(C) - FuncR(Xi+1.0+ pT/80.0));

	Yi = Func(Xi-1.0 + 0.5);
	C = vec2(Xf+1.0, Qt.y - Yi );
	D = min(D, length(C) - FuncR(Xi-1.0+ pT/80.0));

	return min(1.0, D);
}

vec4 main(vec2 fragCoord) {
	vec2 UV = 2.0*(fragCoord.xy - u_resolution.xy/2.0) / min(u_resolution.x, u_resolution.y);
	vec3 Color = mix(u_dominant.rgb * 0.2, u_vibrant.rgb * 0.4, clamp(UV.y, 0.0, 1.0));
	
	for(int i = 0; i < 2; i++) {
        float J = float(i) * 0.5; // 0.0 to 0.5
		float Lt = u_time*(0.5 + 2.0*J)*(1.0 + 0.1*sin(226.0*J)) + 17.0*J;
		vec2 Lp = vec2(0.0, 0.3+1.5*(J - 0.5));
		float L = Layer(UV + Lp, Lt);

		float Blur = 1.0 + 0.5*sin(0.1*u_time);
		Blur *= Blur;
		Blur *= 0.2;
		float V = mix( 0.0, 1.0, 1.0 - smoothstep( 0.0, 0.01 +0.2*Blur, L ) );
		vec3 Lc = mix( u_vibrant.rgb, vec3(1.0), J);

		Color = mix(Color, Lc, V);
	}
    Color += u_energy * 0.3 * u_vibrant.rgb;
	return vec4(Color, 1.0);
}
"""

@Composable
actual fun TerrariaWaterBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val lifecycleState by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val isActiveApp = lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
    
    var time by remember { mutableFloatStateOf(0f) }
    val currentIsPlayerScreen by rememberUpdatedState(isPlayerScreen)
    
    LaunchedEffect(isActiveApp) {
        if (!isActiveApp) return@LaunchedEffect
        var lastTime = 0L
        while (true) {
            withFrameMillis { frameTime ->
                if (lastTime == 0L) lastTime = frameTime
                val dt = ((frameTime - lastTime) / 1000f).coerceAtMost(0.1f)
                lastTime = frameTime
                
                time += dt
            }
            if (!currentIsPlayerScreen) kotlinx.coroutines.delay(24L)
        }
    }

    val bassAmplitudesState = visualizerManager.bassAmplitudes.collectAsState()

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
                    val effectiveTime = if (isPlayerScreen) time * 0.6f else time * 0.25f
                    
                    val bassAmplitudes = bassAmplitudesState.value
                    var bassAvg = 0f
                    if (bassAmplitudes.isNotEmpty()) bassAvg = bassAmplitudes.average().toFloat().let { if (it.isNaN()) 0f else it }
                    val effectiveEnergy = if (isPlayerScreen) bassAvg * 0.3f else 0f
                    
                    runtimeShader.setFloatUniform("u_resolution", size.width, size.height)
                    runtimeShader.setFloatUniform("u_time", effectiveTime)
                    runtimeShader.setFloatUniform("u_energy", effectiveEnergy)
                    runtimeShader.setFloatUniform("u_is_player", if (isPlayerScreen) 1f else 0f)
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
