import org.jetbrains.skia.RuntimeEffect

fun main() {
    val sksl = """
uniform float2 u_resolution;
uniform float u_time;
uniform float4 u_dominant;
uniform float4 u_vibrant;
uniform float u_energy;

#define TAU 6.28318530718

float Func(float pX) {
	return 0.6*(0.5*sin(0.1*pX) + 0.5*sin(0.553*pX) + 0.7*sin(1.2*pX));
}

float FuncR(float pX) {
	return 0.5 + 0.25*(1.0 + sin(mod(40.0*pX, TAU)));
}

float Layer(float2 pQ, float pT) {
	float2 Qt = 3.5*pQ;
	pT *= 0.5;
	Qt.x += pT;

	float Xi = floor(Qt.x);
	float Xf = Qt.x - Xi - 0.5;

	float2 C;
	float Yi;
	float D = 1.0 - step(Qt.y, Func(Qt.x));

	Yi = Func(Xi + 0.5);
	C = float2(Xf, Qt.y - Yi );
	D = min(D, length(C) - FuncR(Xi+ pT/80.0));

	Yi = Func(Xi+1.0 + 0.5);
	C = float2(Xf-1.0, Qt.y - Yi );
	D = min(D, length(C) - FuncR(Xi+1.0+ pT/80.0));

	Yi = Func(Xi-1.0 + 0.5);
	C = float2(Xf+1.0, Qt.y - Yi );
	D = min(D, length(C) - FuncR(Xi-1.0+ pT/80.0));

	return min(1.0, D);
}

half4 main(float2 fragCoord) {
	float2 UV = 2.0*(fragCoord.xy - u_resolution.xy/2.0) / min(u_resolution.x, u_resolution.y);
	float3 Color = mix(u_dominant.rgb * 0.2, u_vibrant.rgb * 0.4, clamp(UV.y, 0.0, 1.0));

	for(int i = 0; i <= 5; i++) {
        float J = float(i) * 0.2;
		float Lt = u_time*(0.5 + 2.0*J)*(1.0 + 0.1*sin(226.0*J)) + 17.0*J;
		float2 Lp = float2(0.0, 0.3+1.5*(J - 0.5));
		float L = Layer(UV + Lp, Lt);

		float Blur = 1.0 + 0.5*sin(0.1*u_time);
		Blur *= Blur;
		Blur *= 0.2;
		float V = mix( 0.0, 1.0, 1.0 - smoothstep( 0.0, 0.01 +0.2*Blur, L ) );
		float3 Lc = mix( u_vibrant.rgb, float3(1.0), J);

		Color = mix(Color, Lc, V);
	}
    Color += u_energy * 0.3 * u_vibrant.rgb;
	return half4(half3(Color), 1.0);
}
"""
    try {
        val effect = RuntimeEffect.makeForShader(sksl)
        println("Compiled successfully")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
