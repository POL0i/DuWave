package com.example.beatpulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.beatpulse.data.AppPreferences
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.backgrounds.*
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager
import com.example.beatpulse.utils.getLocalizedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

private val DummyVisualizerManager = object : IAudioVisualizerManager {
    override val bassAmplitudes: StateFlow<FloatArray> = MutableStateFlow(FloatArray(0))
    override val midAmplitudes: StateFlow<FloatArray> = MutableStateFlow(FloatArray(0))
    override val highAmplitudes: StateFlow<FloatArray> = MutableStateFlow(FloatArray(0))
    override val combinedAmplitudes: StateFlow<FloatArray> = MutableStateFlow(FloatArray(0))
    override val isAdvancedMode: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val filterMode: MutableStateFlow<Any> = MutableStateFlow(Any())
    override val sensitivity: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val reactivity: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val damping: MutableStateFlow<Float> = MutableStateFlow(0.8f)
    override val bassMultiplier: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val midMultiplier: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val trebleMultiplier: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val visualizerArchetype: MutableStateFlow<Int> = MutableStateFlow(0)
    override val fftMode: MutableStateFlow<String> = MutableStateFlow("")
}

@Composable
fun DesignSettingsDialog(
    prefs: AppPreferences,
    paletteColors: PaletteColors,
    dynamicTextColor: Color,
    currentShapeIdx: Int,
    currentBgStyle: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                getLocalizedString("design_settings").takeIf { it.isNotBlank() && it != "design_settings" } ?: "Ajustes de Diseño",
                color = dynamicTextColor,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Shape Selector
                Text(
                    getLocalizedString("thumbnail_shape").takeIf { it.isNotBlank() && it != "thumbnail_shape" } ?: "Marco de Miniatura",
                    color = dynamicTextColor,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val shapes = mutableListOf(
                        Icons.Default.Circle to 0,
                        Icons.Default.CropSquare to 1,
                        Icons.Default.RoundedCorner to 2,
                        Icons.Default.Crop to 3
                    )
                    val isPatreonUnlocked by prefs.isPatreonUnlockedFlow.collectAsState()
                    if (isPatreonUnlocked) {
                        shapes.add(Icons.Default.AccountBalance to 4)
                        shapes.add(Icons.Default.PlayArrow to 5)
                        shapes.add(Icons.Default.Settings to 6)
                    }
                    
                    shapes.forEach { (icon, idx) ->
                        val isSelected = currentShapeIdx == idx
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) paletteColors.vibrant.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { prefs.thumbnailShape = idx }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) paletteColors.vibrant else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                
                Divider(color = dynamicTextColor.copy(alpha = 0.1f))
                
                // Style Selector
                Text(
                    getLocalizedString("visual_style").takeIf { it.isNotBlank() && it != "visual_style" } ?: "Estilo Visual",
                    color = dynamicTextColor,
                    style = MaterialTheme.typography.titleMedium
                )
                
                val styles = listOf(
                    0 to (getLocalizedString("style_classic").takeIf { it != "style_classic" } ?: "Clásico"),
                    1 to (getLocalizedString("style_cyberpunk").takeIf { it != "style_cyberpunk" } ?: "Cyberpunk"),
                    2 to (getLocalizedString("style_anime").takeIf { it != "style_anime" } ?: "Anime"),
                    3 to (getLocalizedString("style_luminous").takeIf { it != "style_luminous" } ?: "Luminoso"),
                    4 to (getLocalizedString("style_kawaii").takeIf { it != "style_kawaii" } ?: "Kawaii"),
                    5 to (getLocalizedString("style_black_metal").takeIf { it != "style_black_metal" } ?: "Black Metal"),
                    6 to (getLocalizedString("style_dark_fantasy").takeIf { it != "style_dark_fantasy" } ?: "Fantasía Oscura"),
                    7 to (getLocalizedString("style_cathedral").takeIf { it != "style_cathedral" } ?: "Catedral"),
                    8 to (getLocalizedString("style_tale_legend").takeIf { it != "style_tale_legend" } ?: "Leyenda")
                ).map { (id, name) -> id to name.removePrefix("Estilo: ").trim() }.toMutableList()
                
                val isPatreonUnlocked by prefs.isPatreonUnlockedFlow.collectAsState()
                if (isPatreonUnlocked) {
                    styles.add(9 to "Muro Patreon")
                    styles.add(10 to "Fuente Oscura")
                    styles.add(11 to "Terraria")
                    styles.add(12 to "Zen Clear")
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 90.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    
                    items(styles) { item ->
                        val idx = item.first
                        val name = item.second
                        val isSelected = currentBgStyle == idx
                        
                        StyleGridItem(
                            idx = idx,
                            name = name,
                            isSelected = isSelected,
                            paletteColors = paletteColors,
                            dynamicTextColor = dynamicTextColor,
                            prefs = prefs
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(getLocalizedString("close").takeIf { it != "close" } ?: "Cerrar", color = paletteColors.vibrant)
            }
        },
        containerColor = paletteColors.dominant
    )
}

@Composable
fun StyleGridItem(
    idx: Int,
    name: String,
    isSelected: Boolean,
    paletteColors: PaletteColors,
    dynamicTextColor: Color,
    prefs: AppPreferences
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { prefs.backgroundStyle = idx }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Square box for preview
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            // Real preview using the component
            Box(modifier = Modifier.fillMaxSize()) {
                when (idx) {
                    1 -> CyberpunkBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    2 -> AnimeBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    3 -> LuminousBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    4 -> Y2KBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    5 -> DarkAmbientBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    6 -> GothicFantasyBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    7 -> CathedralFantasyBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    8 -> TaleLegendBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    9 -> RetroWallBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    10 -> FountainBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    11 -> TerrariaWaterBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    12 -> ZenClearBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    else -> Box(modifier = Modifier.fillMaxSize().background(paletteColors.dominant))
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
            color = if (isSelected) paletteColors.vibrant else dynamicTextColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

