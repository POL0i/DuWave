package com.example.beatpulse.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PixelIcons {
    val Library = ImageVector.Builder(
        name = "PixelLibrary",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(4f, 4f)
        lineTo(4f, 20f)
        lineTo(8f, 20f)
        lineTo(8f, 4f)
        close()
        moveTo(10f, 4f)
        lineTo(10f, 20f)
        lineTo(14f, 20f)
        lineTo(14f, 4f)
        close()
        moveTo(16f, 4f)
        lineTo(16f, 20f)
        lineTo(20f, 20f)
        lineTo(20f, 4f)
        close()
    }.build()

    val Folder = ImageVector.Builder(
        name = "PixelFolder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(2f, 6f)
        lineTo(10f, 6f)
        lineTo(12f, 8f)
        lineTo(22f, 8f)
        lineTo(22f, 20f)
        lineTo(2f, 20f)
        close()
    }.build()

    val Play = ImageVector.Builder(
        name = "PixelPlay",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(8f, 6f)
        lineTo(8f, 18f)
        lineTo(18f, 12f)
        close()
    }.build()

    val Pause = ImageVector.Builder(
        name = "PixelPause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(6f, 6f)
        lineTo(10f, 6f)
        lineTo(10f, 18f)
        lineTo(6f, 18f)
        close()
        moveTo(14f, 6f)
        lineTo(18f, 6f)
        lineTo(18f, 18f)
        lineTo(14f, 18f)
        close()
    }.build()

    val SkipNext = ImageVector.Builder(
        name = "PixelSkipNext",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(6f, 6f)
        lineTo(6f, 18f)
        lineTo(14f, 12f)
        close()
        moveTo(14f, 6f)
        lineTo(18f, 6f)
        lineTo(18f, 18f)
        lineTo(14f, 18f)
        close()
    }.build()

    val SkipPrev = ImageVector.Builder(
        name = "PixelSkipPrev",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(10f, 12f)
        lineTo(18f, 18f)
        lineTo(18f, 6f)
        close()
        moveTo(6f, 6f)
        lineTo(10f, 6f)
        lineTo(10f, 18f)
        lineTo(6f, 18f)
        close()
    }.build()
    
    val Settings = ImageVector.Builder(
        name = "PixelSettings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(8f, 8f)
        lineTo(16f, 8f)
        lineTo(16f, 16f)
        lineTo(8f, 16f)
        close()
        moveTo(4f, 10f)
        lineTo(8f, 10f)
        lineTo(8f, 14f)
        lineTo(4f, 14f)
        close()
        moveTo(16f, 10f)
        lineTo(20f, 10f)
        lineTo(20f, 14f)
        lineTo(16f, 14f)
        close()
        moveTo(10f, 4f)
        lineTo(14f, 4f)
        lineTo(14f, 8f)
        lineTo(10f, 8f)
        close()
        moveTo(10f, 16f)
        lineTo(14f, 16f)
        lineTo(14f, 20f)
        lineTo(10f, 20f)
        close()
    }.build()
    
    val Info = ImageVector.Builder(
        name = "PixelInfo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(10f, 4f)
        lineTo(14f, 4f)
        lineTo(14f, 8f)
        lineTo(10f, 8f)
        close()
        moveTo(10f, 10f)
        lineTo(14f, 10f)
        lineTo(14f, 20f)
        lineTo(10f, 20f)
        close()
    }.build()

    val Shuffle = ImageVector.Builder(
        name = "PixelShuffle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(4f, 6f)
        lineTo(8f, 6f)
        lineTo(16f, 18f)
        lineTo(20f, 18f)
        lineTo(20f, 14f)
        lineTo(22f, 19f)
        lineTo(20f, 24f)
        lineTo(20f, 20f)
        lineTo(14f, 20f)
        lineTo(6f, 8f)
        lineTo(4f, 8f)
        close()
        moveTo(16f, 6f)
        lineTo(20f, 6f)
        lineTo(20f, 10f)
        lineTo(22f, 5f)
        lineTo(20f, 0f)
        lineTo(20f, 4f)
        lineTo(14f, 4f)
        lineTo(12f, 7f)
        lineTo(13.5f, 9f)
        close()
        moveTo(8f, 18f)
        lineTo(4f, 18f)
        lineTo(4f, 20f)
        lineTo(9.5f, 20f)
        lineTo(11f, 17.5f)
        lineTo(9.5f, 15.5f)
        close()
    }.build()
}
