package com.example.beatpulse.ui.utils

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val CathedralShape = GenericShape { size, _ ->
    moveTo(0f, size.height)
    lineTo(0f, size.height * 0.4f)
    quadraticTo(0f, 0f, size.width / 2f, 0f)
    quadraticTo(size.width, 0f, size.width, size.height * 0.4f)
    lineTo(size.width, size.height)
    close()
}

val DiamondShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

val HexagonShape = GenericShape { size, _ ->
    moveTo(size.width * 0.5f, 0f)
    lineTo(size.width, size.height * 0.25f)
    lineTo(size.width, size.height * 0.75f)
    lineTo(size.width * 0.5f, size.height)
    lineTo(0f, size.height * 0.75f)
    lineTo(0f, size.height * 0.25f)
    close()
}

fun getShapeForIndex(index: Int): Shape {
    return when (index) {
        1 -> RoundedCornerShape(0.dp)
        2 -> RoundedCornerShape(16.dp)
        3 -> RoundedCornerShape(32.dp)
        4 -> CathedralShape
        5 -> DiamondShape
        6 -> HexagonShape
        else -> CircleShape
    }
}
