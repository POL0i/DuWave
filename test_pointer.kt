import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isSecondaryPressed

fun check(event: PointerEvent) {
    if (event.buttons.isSecondaryPressed) {
        println("Secondary pressed!")
    }
}
