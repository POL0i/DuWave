import org.jetbrains.skia.RuntimeEffect
fun main() {
    val src = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float iEnergy;
        uniform half4 colorDominant;
        uniform half4 colorVibrant;
        uniform half4 colorMuted;
        uniform float iOffsetY;

        mat2 rot(float a) {
            float s = sin(a), c = cos(a);
            return mat2(c, -s, s, c);
        }

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord.xy - 0.5 * iResolution.xy + float2(0.0, iOffsetY * iResolution.y)) / iResolution.y;
            return half4(1.0, 0.0, 0.0, 1.0);
        }
    """
    try {
        val effect = RuntimeEffect.makeForShader(src)
        println("Success!")
    } catch (e: Exception) {
        println("Error: " + e.message)
    }
}
