import javax.sound.sampled.AudioSystem
import java.io.File
import java.net.URI

fun main() {
    println("Testing AudioSystem")
    try {
        val formats = AudioSystem.getAudioFileTypes()
        println("Supported formats: ${formats.joinToString()}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
