import com.kmpalette.palette.graphics.Palette
import androidx.compose.ui.graphics.ImageBitmap

fun test(imageBitmap: ImageBitmap) {
    val palette = Palette.from(imageBitmap).generate()
    val dom = palette.dominantSwatch?.rgb
    val vib = palette.vibrantSwatch?.rgb
}
