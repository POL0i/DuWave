package com.example.beatpulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import kotlinx.coroutines.launch
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
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = paletteColors.dominant,
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.isAltPressed) {
                        false
                    } else false
                }
                .fillMaxWidth(0.9f)
                .widthIn(max = 360.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    getLocalizedString("design_settings").takeIf { it.isNotBlank() && it != "design_settings" } ?: "Ajustes de Diseño",
                    color = dynamicTextColor,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val styles = listOf(
                    0 to (getLocalizedString("style_classic").takeIf { it != "style_classic" } ?: "Clásico"),
                    1 to (getLocalizedString("style_cyberpunk").takeIf { it != "style_cyberpunk" } ?: "Cyberpunk"),
                    2 to (getLocalizedString("style_anime").takeIf { it != "style_anime" } ?: "Anime"),
                    3 to (getLocalizedString("style_luminous").takeIf { it != "style_luminous" } ?: "Luminoso"),
                    4 to (getLocalizedString("style_kawaii").takeIf { it != "style_kawaii" } ?: "Kawaii"),
                    5 to (getLocalizedString("style_black_metal").takeIf { it != "style_black_metal" } ?: "Black Metal"),
                    6 to (getLocalizedString("style_dark_fantasy").takeIf { it != "style_dark_fantasy" } ?: "Fantasía Oscura"),
                    7 to (getLocalizedString("style_cathedral").takeIf { it != "style_cathedral" } ?: "Catedral"),
                    8 to (getLocalizedString("style_tale_legend").takeIf { it != "style_tale_legend" } ?: "Leyenda"),
                    14 to "Ojos Lullaby"
                ).map { (id, name) -> id to name.removePrefix("Estilo: ").trim() }.toMutableList()
                
                val isPatreonUnlocked by prefs.isPatreonUnlockedFlow.collectAsState()
                if (isPatreonUnlocked) {
                    styles.add(9 to "Muro Patreon")
                    styles.add(10 to "Fuente Oscura")
                    styles.add(11 to "Terraria")
                    styles.add(12 to "Zen Clear")
                    styles.add(13 to "Mareas de Arena")
                }
                
                val chunkedStyles = styles.chunked(4)
                val pagerState = rememberPagerState(pageCount = { chunkedStyles.size })
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp) // Más largo, ensures all 7 shapes fit without clipping
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.isAltPressed) {
                                if (event.key == Key.DirectionRight) {
                                    val next = (pagerState.currentPage + 1) % pagerState.pageCount
                                    coroutineScope.launch { pagerState.animateScrollToPage(next) }
                                    return@onKeyEvent true
                                } else if (event.key == Key.DirectionLeft) {
                                    val prev = if (pagerState.currentPage - 1 < 0) pagerState.pageCount - 1 else pagerState.currentPage - 1
                                    coroutineScope.launch { pagerState.animateScrollToPage(prev) }
                                    return@onKeyEvent true
                                }
                            }
                            false
                        },
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Thumbnail Shapes
                    Column(
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val shapeIndices = mutableListOf(0, 1, 2, 3)
                        if (isPatreonUnlocked) {
                            shapeIndices.addAll(listOf(4, 5, 6))
                        }
                        
                        shapeIndices.forEach { idx ->
                            val isSelected = currentShapeIdx == idx
                            val shapeForThumb = when (idx) {
                                1 -> androidx.compose.ui.graphics.RectangleShape
                                2 -> RoundedCornerShape(4.dp)
                                3 -> RoundedCornerShape(8.dp)
                                4 -> CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
                                5 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                6 -> CutCornerShape(8.dp)
                                else -> CircleShape
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(shapeForThumb)
                                    .background(if (isSelected) paletteColors.vibrant.copy(alpha = 0.5f) else Color.Transparent)
                                    .clickable { prefs.thumbnailShape = idx }
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.3f),
                                        shape = shapeForThumb
                                    )
                            )
                        }
                    }
                    
                    // Right Column: Visual Styles
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(
                            getLocalizedString("visual_style").takeIf { it.isNotBlank() && it != "visual_style" } ?: "Estilo Visual",
                            color = dynamicTextColor,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) { page ->
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(chunkedStyles[page]) { item ->
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
                        
                        // Dots and Close button aligned at the bottom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left spacer to push dots to center relative to the available space
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Dots
                            Row(horizontalArrangement = Arrangement.Center) {
                                repeat(chunkedStyles.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.3f)
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(10.dp)
                                            .clickable {
                                                coroutineScope.launch { pagerState.animateScrollToPage(iteration) }
                                            }
                                    )
                                }
                            }
                            
                            // Close Button
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                Text(
                                    text = getLocalizedString("close").takeIf { it != "close" } ?: "Cerrar",
                                    color = paletteColors.vibrant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onDismiss() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
                .height(115.dp) // Estirado para rellenar el espacio vacío
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
                    13 -> SandsFlowBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                    14 -> com.example.beatpulse.ui.components.backgrounds.LullabyEyesBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
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

